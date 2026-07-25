# Webhook subscriptions — `client.hooks()`

Erli notifies a shop about events by calling an HTTP endpoint the shop registers: a **hook**. This
page covers managing those subscriptions and firing them on demand. If your shop cannot expose a
public endpoint, read [`inbox.md`](inbox.md) instead — the inbox delivers the same events by polling.

Consumers import only `io.github.mgrtomaszzurawski.erli.domain.hooks`.

## The five subscriptions

`HookKind` is the closed set of events Erli can call you about:

| Kind | Erli calls you to… |
|---|---|
| `CHECK_BUYABILITY` | ask whether products are still sellable, and in what quantity |
| `PRODUCTS_NEED_SYNC` | tell you which products need re-synchronising |
| `ORDER_CREATED` | announce a new order |
| `ORDER_STATUS_CHANGED` | announce a marketplace-side order status change |
| `ORDER_SELLER_STATUS_CHANGED` | announce a seller-side order status change |

## Register and inspect

```java
try (ErliClient client = ErliClient.fromEnvironment()) {
    client.hooks().save(Hook.of(
            HookKind.ORDER_CREATED,
            URI.create("https://your-shop.example/erli/order-created"),
            System.getenv("SHOP_HOOK_TOKEN")));   // optional: echoed back so you can authorise the call

    for (Hook hook : client.hooks().list()) {
        System.out.println(hook);   // toString() redacts the token and the URL's query string
    }

    client.hooks().delete(HookKind.ORDER_CREATED);
}
```

A hook you register must use `https` and must not carry credentials in its userinfo: Erli sends the access token
and, for the three `ORDER_*` kinds, the buyer's personal data to this endpoint. `Hook.toString()`
redacts the token *and* the URL's query string, since a webhook URL often carries the shared secret
there.

A subscription already stored on the shop is read back as-is even if it breaks those rules, so
`list()` can still show you an insecure hook you need to replace.

`save` creates or overwrites the subscription named by `hook.kind()` — there is one subscription per
kind, so saving twice replaces rather than duplicates. `delete` succeeds even when nothing was
registered.

The access token is a credential of *your* system. `Hook.toString()` redacts it; read
`hook.accessToken()` when you genuinely need the value. Note that `list()` returns it, so treat the
result as sensitive.

## Firing a hook on demand

The two `run` operations make **Erli call your registered endpoint** and report what happened. They
exercise your integration; they do not read Erli's data.

```java
List<ProductBuyability> answers = client.hooks().checkBuyability(List.of(
        BuyabilityQuery.of(ProductExternalId.of("SKU-1"), 2),
        BuyabilityQuery.of(ProductExternalId.of("SKU-2"), 1)));

for (ProductBuyability answer : answers) {
    answer.status().ifPresent(status -> System.out.println(answer.productId() + " is " + status));
    answer.stock().ifPresent(stock -> System.out.println("  stock: " + stock));
}
```

Both components are `Optional`: the API declares `status` explicitly nullable, and a shop may report
availability without a stock figure. With no `CHECK_BUYABILITY` subscription registered the call
succeeds and returns an empty list rather than failing.

```java
// Ask Erli to call your PRODUCTS_NEED_SYNC endpoint for these products.
client.hooks().notifyProductsNeedSync(ProductSyncNotification.ofProducts(
        List.of(ProductExternalId.of("SKU-1"), ProductExternalId.of("SKU-2"))));

// …or only for particular fields; an empty field list means the whole product.
client.hooks().notifyProductsNeedSync(ProductSyncNotification.ofFields(
        List.of(ProductExternalId.of("SKU-1")), List.of("price")));
```

One call may name between 1 and 1000 products; `ProductSyncNotification` rejects anything outside
that range before a request is sent. Firing this hook also raises a `PRODUCTS_NEED_SYNC` message in
the shop's inbox.

## Errors

Every operation throws the remediation exception matching the failure: `ErliAuthException` (401/403),
`ErliNotFoundException` (404), `ErliValidationException` (400/409/422), `ErliServerException` (5xx).
Each carries `details()` with `traceId`/`spanId` and the API's `polishMessage`.
