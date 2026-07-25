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

A hook you register must use `https` and must not carry credentials in its userinfo: Erli sends the
access token and, for the three `ORDER_*` kinds, the buyer's personal data to this endpoint. `Hook.toString()`
redacts the token *and* the URL's query string, since a webhook URL often carries the shared secret
there.

A subscription already stored on the shop is read back as-is even if it breaks those rules, so
`list()` can still show you an insecure hook you need to replace.

`list()` throws `ErliTransportException` if the shop holds a subscription this SDK version cannot
represent, and because the list is mapped as a whole you then see none of them. Two causes, with
different remedies:

- **A URL the SDK rejects** (over the length limit, or malformed). The message names the hook kind, so
  `save` a corrected subscription for that kind, or `delete(HookKind)` it — neither needs the stored
  value to decode.
- **A hook name newer than this SDK.** The message cannot name it, because there is no `HookKind`
  constant for a name the SDK does not know — and for the same reason `delete(HookKind)` cannot address
  it either. Upgrade the SDK, or remove that subscription from the shop panel.

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
availability without a stock figure.

An empty `status` has two causes and they are indistinguishable here: the shop did not state one, or
it stated one newer than your SDK version (an unrecognised value decodes to absent — see
[`inbox.md`](inbox.md)). Either way, do not read it as "inactive"; treat it as "not stated".

With no `CHECK_BUYABILITY` subscription registered the call succeeds and returns an empty list rather
than failing.

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

| HTTP | Exception |
|---|---|
| 401 / 403 | `ErliAuthException` |
| 404 | `ErliNotFoundException` |
| 400 / 409 / 422 | `ErliValidationException` |
| 5xx | `ErliServerException` |

All extend `ErliException` and carry `details()` with `traceId`/`spanId` and the API's Polish message
where it supplied one. A stored subscription the SDK cannot represent is reported as
`ErliTransportException` (see `list()` above), so catching `ErliException` covers every failure of a
call.
