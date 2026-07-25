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

Erli adds enum values (carriers, tax rates, statuses) faster than a released SDK can vendor them. The
SDK never fails a whole page over one such value: an unrecognised value decodes as **absent**, so an
`Optional` component simply comes back empty.

That means an empty `Optional` has two possible causes — the API sent nothing, or it sent something
newer than this SDK. Where it matters you can tell them apart from the siblings. For the carrier:

```java
DeliveryTracking tracking = order.deliveryTracking().orElseThrow();
if (tracking.vendor().isEmpty() && tracking.trackingNumber().isPresent()) {
    // The API always sends carrier and tracking number together, so a tracking number without a
    // carrier means the carrier is newer than this SDK. The number still identifies the parcel;
    // upgrade the SDK to get the carrier itself.
    log.warn("unknown carrier for parcel {}", tracking.trackingNumber().get());
}
```

The affected components are `DeliveryTracking.vendor()`, `OrderLine.taxRate()`,
`PickupPlace.provider()` and `OrderPaymentSummary.status()`; each says so in its own javadoc.

A **required** property is different: the SDK cannot build the record without it, so the message fails
to map and the exception names the property and says it is absent *or* unrecognised — it will not
claim the property was missing, because at that point the SDK genuinely cannot tell. That failure is
per message and it names the message id, so you can acknowledge that one and keep draining (see the
loop above).

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

As elsewhere in the SDK: `ErliAuthException` (401/403), `ErliNotFoundException` (404),
`ErliValidationException` (400/409/422), `ErliServerException` (5xx), each carrying `traceId`/`spanId`
and the API's `polishMessage`.
