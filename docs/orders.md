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

`Buyer`, `DeliveryAddress`, `InvoiceAddress` and `BankAccount` are personal data. Their `toString()`
renders only the type name, and `Order.toString()` therefore never contains a buyer's name, address,
phone, e-mail or account number. Accessors return the real values — the redaction guards accidental
disclosure through logging, not deliberate use.

```java
log.info("processing {}", order);          // safe: no personal data in the rendering
String city = order.buyer()                // deliberate access still works
        .map(buyer -> buyer.deliveryAddress().city())
        .orElse("unknown");
```

## Errors

| HTTP | Exception |
|---|---|
| 401 / 403 | `ErliAuthException` |
| 404 | `ErliNotFoundException` |
| 400 / 409 / 422 | `ErliValidationException` |
| 5xx | `ErliServerException` |

All extend `ErliException` and carry `details()` with the Erli code, `traceId`/`spanId` and the
original Polish message where the API supplied one.

## Optionality

Fields Erli documents as optional are `Optional` (or `OptionalInt`/`OptionalLong`) rather than
nullable, and `items()`/`returns()` are always present — empty rather than null. `deliveryTracking`
merges Erli's two shapes into one record: `status` is always set, while `vendor` +`trackingNumber`
or `trackingUrl` are populated depending on what the carrier reported.
