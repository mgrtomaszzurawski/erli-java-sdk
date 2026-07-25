# Orders

Read and update the shop's orders through `client.orders()`. Four operations are covered:
`POST /orders/_search`, `GET /orders/{id}`, `PATCH /orders/{id}` and `PATCH /orders/{id}/status`.

```java
try (ErliClient client = ErliClient.fromEnvironment()) {
    Order order = client.orders().byId(OrderId.of("221201x12345"));
    System.out.println(order.totalPrice().amount() + " " + order.totalPrice().currency());
}
```

## Searching

`search` returns a **lazy** `Stream<Order>` that walks every matching page. Pages are fetched as the
stream is consumed, so a short-circuiting operation only pays for what it reads —
`search(request).limit(10)` performs a single request.

```java
List<Order> recent = client.orders()
        .search(OrderSearchRequest.builder()
                .filter(OrderFilter.updatedAfter(lastSync))
                .sortBy(OrderSearchRequest.SortField.UPDATED)
                .direction(OrderSearchRequest.SortDirection.ASCENDING)
                .pageSize(200)
                .build())
        .limit(500)
        .toList();
```

Page size tunes how often the stream goes to the network (1–200, default 50); it does not limit the
result set. Consume the stream before closing the client.

`_search` is a read, but Erli exposes it as a `POST`, so it is not retried by default — a single
transient 429 or 503 aborts a long sync. For an unattended job either opt in with
`RetryPolicy.builder().retryPost(true)`, or catch the failure and resume from the last cursor you
processed (below).

### Resuming a walk

Every `Order` carries the `cursor` to continue after it. Store the last one you processed and resume
later instead of re-reading from the beginning — the usual incremental-sync loop:

```java
OrderSearchRequest.Builder request = OrderSearchRequest.builder().filter(OrderFilter.paymentCompleted());
lastSeenCursor.ifPresent(request::startAfter);
```

### Filters

`OrderFilter` mirrors Erli's recursive filter grammar, so predicates nest arbitrarily:

```java
OrderFilter filter = OrderFilter.and(
        OrderFilter.paymentCompleted(),
        OrderFilter.or(
                OrderFilter.idIn(List.of(OrderId.of("221201x1"), OrderId.of("221201x2"))),
                OrderFilter.not(OrderFilter.userEmail("buyer@example.com"))));
```

Beyond the shorthands (`createdAfter`, `updatedAfter`, `paymentCompleted`, `userEmail`, `idIn`) there
are the general `compare`, `equalTo`, `in`, `notIn`, `and`, `or` and `not`. Filterable fields are
`id`, `created`, `updated`, `paymentStatus` and `userEmail`.

## Updating

Two statuses travel with an order and mean different things. `Order.status()` is the marketplace's
own view and is **read-only**. `Order.sellerStatus()` is the status in *your* system, and it is the
one the API lets you write:

```java
client.orders().changeStatus(orderId, SellerStatus.SENT);
```

`update` applies a partial change — currently your own identifier for the order:

```java
client.orders().update(orderId, OrderUpdateRequest.ofExternalOrderId("ERP-1001"));
```

A request that would change nothing is rejected before it reaches the network. A cancelled order can
no longer be updated; Erli answers such a call with `ErliValidationException`.

Erli's update payload also accepts a `deliveryTracking` block, which the API itself deprecates. This
SDK does not surface it — set the status with `changeStatus` above, and register a consignment
through the shipping API instead.

## Money

Erli sends every amount as an integer count of minor units (grosze) with the order's currency
alongside. The SDK rebuilds them as `Money` at the currency's own scale, so `12787` in `PLN` reaches
you as `127.87`:

```java
Money total = order.totalPrice();          // 127.87 PLN
Money unit  = order.items().get(0).unitPrice();
```

Item prices are **per unit** — multiply by `quantity()` for a line total.

## Buyer personal data

`Buyer`, `DeliveryAddress`, `InvoiceAddress` and `BankAccount` are personal data, and their
`toString()` renders only the type name.

`Order.toString()` and `OrderReturn.toString()` go further and render a curated subset — identity,
state, totals, counts and timestamps. They deliberately omit the buyer, the pickup place (a street
address), and **the free-text comments**, because `Order.comment()` and `OrderReturn.comment()` are
written by the buyer and that is exactly where a phone number ends up. Accessors return the real
values throughout — the redaction guards accidental disclosure through logging, not deliberate use.

```java
log.info("processing {}", order);          // safe: renders no personal data
String city = order.buyer()                // deliberate access still works
        .map(buyer -> buyer.deliveryAddress().city())
        .orElse("unknown");
```

Two things are still yours to handle: anything you log from an accessor, and `OrderFilter`, whose
`toString()` shows the value you filtered on — which may be a buyer's e-mail address.

## Errors

| HTTP | Exception |
|---|---|
| 401 / 403 | `ErliAuthException` |
| 404 | `ErliNotFoundException` |
| 400 / 409 / 422 | `ErliValidationException` |
| 5xx | `ErliServerException` |

All extend `ErliException` and carry `details()` with the Erli code, `traceId`/`spanId` and the
original Polish message where the API supplied one. A response that omits a field the spec marks
required is reported as `ErliTransportException`, naming the field — so catching `ErliException`
covers every failure of a call.

## Optionality

Fields Erli documents as optional are `Optional` (or `OptionalInt`/`OptionalLong`) rather than
nullable, and `items()`/`returns()` are always present — empty rather than null. `deliveryTracking`
merges Erli's two shapes into one record: `status` is always set, while `vendor` + `trackingNumber`
or `trackingUrl` are populated depending on what the carrier reported.

## Carriers

`DeliveryTracking.vendor()` is a `core.model.DeliveryVendor` — the shared carrier type, the same one
the shipping, dictionaries and inbox APIs use, so a carrier means the same thing everywhere:

```java
tracking.vendor().ifPresent(vendor -> myErp.setCarrier(vendor.wireValue()));
if (tracking.vendor().filter(DeliveryVendor.INPOST::equals).isPresent()) { ... }
```

One caveat worth knowing. Erli's carrier list grows, and the SDK vendors the API spec — so a carrier
added after your SDK version was built decodes to *no vendor* rather than failing. Because `vendor` is
also legitimately absent on URL-only tracking, the two cases are indistinguishable from this record.
If a parcel has a `trackingNumber` but no `vendor`, suspect a carrier newer than your SDK and upgrade;
do not treat an empty `vendor` as "shipped without a carrier".
