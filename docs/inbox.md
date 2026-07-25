# Event inbox — `client.inbox()`

The inbox is Erli's **pull-based** delivery of the same events the webhook subscriptions push. Use it
when your shop cannot expose a public HTTP endpoint, or as a safety net for events a webhook missed.
For the push side see [`hooks.md`](hooks.md).

Consumers import only `io.github.mgrtomaszzurawski.erli.domain.inbox`.

## The drain loop

Messages arrive **oldest first, at most 500 per call**, and stay unread until you acknowledge them.
That makes the loop explicit: fetch, process, acknowledge, repeat until empty.

```java
try (ErliClient client = ErliClient.fromEnvironment()) {
    List<Message> batch;
    while (!(batch = client.inbox().unread()).isEmpty()) {
        for (Message message : batch) {
            handle(message);
        }
        // The batch is oldest-first, so acknowledging the last id acknowledges all of them.
        int marked = client.inbox().markRead(ReadReceipt.upTo(batch.get(batch.size() - 1).id()));
        if (marked < batch.size()) {
            // Fewer messages were acknowledged than were handed out, so the next call would return
            // some of the same ones. Stop rather than spin.
            throw new IllegalStateException("inbox did not fully advance: marked " + marked
                    + " of " + batch.size());
        }
    }
}
```

`markRead` returns how many messages were actually acknowledged. `upTo` is a **range**: acknowledging
the last id of a batch acknowledges every message up to and including it — verified against the live
API, not inferred.

If only some messages were processed successfully, acknowledge exactly those instead — the rest come
back on the next call:

```java
client.inbox().markRead(ReadReceipt.exactly(processedIds));
```

Acknowledging an id that does not exist is not an error; it simply marks nothing (the call returns `0`).

There is no cursor for these operations, so the SDK returns a plain `List` rather than a lazy
`Stream`: draining means calling `unread()` again after acknowledging.

## Reading a message

`Message.payload()` is a sealed `MessagePayload`, and `Message.type()` says which shape arrived:

```java
void handle(Message message) {
    if (message.payload().isEmpty()) {
        // A message type this SDK version does not know. Log message.typeName() and skip it —
        // the SDK never guesses a payload shape.
        return;
    }
    MessagePayload payload = message.payload().get();
    if (payload instanceof OrderEvent order) {
        System.out.println(message.type() + " for order " + order.id()
                + " total " + order.totalPrice().amount() + " " + order.currency());
    } else if (payload instanceof ProductsSyncEvent sync) {
        resync(sync.productIds(), sync.fields());
    }
}
```

Convenience accessors avoid the cast when you only care about one shape:
`message.orderEvent()` and `message.productsSyncEvent()`, both `Optional`.

## Order events

The three `ORDER_*` types all carry an `OrderEvent`: a **snapshot of the order at the moment the event
was raised**, not a live order. Read `client.orders()` for the current state of `order.id()`.

Money is exposed as `Money` (the API sends integer minor units; the order's `currency` applies to
every amount in the payload). Everything the API declares optional is an `Optional`, including
`buyer()`, `rebate()`, `deliveryTracking()` and `purchasedAt()`.

**Buyer personal data.** `Buyer`, `DeliveryAddress`, `InvoiceAddress` and `BankAccount` redact
themselves in `toString()` — printing an `OrderEvent` will not leak an e-mail, name, street, phone or
account number. The values are available through the record components; handling them is your
responsibility.

## Values this SDK version does not recognise

Erli's enum lists (carriers, tax rates, statuses) grow, and the SDK vendors the API spec — so a value
added after your SDK version was built decodes to **absent** rather than failing: you lose that one
value's identity, not the whole payload.

That means an empty `Optional` has two causes, and for the carrier `trackingNumber` tells them apart
(the API declares carrier and tracking number as required together):

| `vendor` | `trackingNumber` | meaning |
|---|---|---|
| empty | empty | URL-only tracking — read `trackingUrl` |
| empty | present | a carrier newer than your SDK version — upgrade to name it |

```java
DeliveryTracking tracking = order.deliveryTracking().orElseThrow();
if (tracking.vendor().isEmpty() && tracking.trackingNumber().isPresent()) {
    System.out.println("carrier newer than this SDK, parcel " + tracking.trackingNumber().get());
}
```

So do not read an empty `vendor` as "shipped without a carrier".

The affected components are `DeliveryTracking.vendor()`, `OrderLine.taxRate()`,
`PickupPlace.provider()` and `OrderPaymentSummary.status()` here, plus
`ProductBuyability.status()` in [`hooks.md`](hooks.md). Only the carrier has a sibling that
disambiguates it; for the other four an empty value is genuinely indistinguishable, so **do not
substitute a default** — an empty `taxRate()` does not mean 0% VAT, and an empty `provider()` does not
mean the point has no operator.

### When the SDK cannot map a message at all

A **required** enum-typed property is different: the SDK cannot build the record without it. That
message fails to map, and the exception says the property is absent *or* unrecognised rather than
claiming it was missing, because at that point the SDK genuinely cannot tell. (Required properties
that are not enums — text, numbers, timestamps — still report plainly as missing.)

Be aware of the blast radius: the batch is mapped eagerly, so **one unmappable message fails the whole
`unread()` or `search()` call** and you receive none of that batch. The messages are not lost — nothing
was acknowledged, so they come back on the next call — but the loop cannot advance until the offending
message is acknowledged. The failure is an `ErliTransportException` whose message names the offending
message id:

```java
try {
    batch = client.inbox().unread();
} catch (ErliTransportException unmappable) {
    // The message text names the id; acknowledge that one to let the loop advance.
    log.error("inbox stalled: {}", unmappable.getMessage(), unmappable);
    throw unmappable;
}
```

The id is currently only available as text inside the exception message, which is awkward to act on
programmatically; treat this path as an upgrade signal rather than something to automate.

## Filtering

```java
List<Message> onlyOrders = client.inbox().search(
        MessageQuery.ofTypes(Set.of(MessageType.ORDER_CREATED, MessageType.ORDER_STATUS_CHANGED)));

List<Message> everything = client.inbox().search(MessageQuery.all());   // same as unread()
```

The API's filter does **not** accept `ORDER_SELLER_STATUS_CHANGED`, even though messages of that type
exist. `MessageQuery` rejects it immediately with an actionable message rather than letting the server
answer with a validation error; fetch every type and filter client-side if you need it.
`MessageType.filterable()` reports, per constant, whether the filter accepts it.

## Errors

| HTTP | Exception |
|---|---|
| 401 / 403 | `ErliAuthException` |
| 404 | `ErliNotFoundException` |
| 400 / 409 / 422 | `ErliValidationException` |
| 5xx | `ErliServerException` |

All extend `ErliException` and carry `details()` with `traceId`/`spanId` and the API's Polish message
where it supplied one. A response the SDK cannot map — a required property absent or unrecognised — is
reported as `ErliTransportException`, so catching `ErliException` covers every failure of a call.
