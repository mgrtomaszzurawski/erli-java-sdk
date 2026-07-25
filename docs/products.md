# Product catalog — `client.products()`

Everything a seller publishes lives here: creating and updating offers, searching the catalog, and
running timed promotions. Consumers import only `io.github.mgrtomaszzurawski.erli.domain.products`.

Erli has **no separate internal product id you create against** — you choose the key. Every operation
addresses a product by its seller-assigned `ProductExternalId`, and the same id is what appears on
orders later.

## Creating a product

Five fields are required on create. The SDK checks them locally, so a missing one is an
`IllegalStateException` naming every gap at once rather than a round trip and a 400.

```java
try (ErliClient client = ErliClient.fromEnvironment()) {
    ProductDraft draft = ProductDraft.of(ProductContent.builder()
            .name("Kurtka zimowa z kapturem")
            .price(Money.ofPln("100.00"))
            .stock(10)
            .dispatchTime(DispatchTime.ofDays(1))
            .images(List.of(ProductImage.of("https://example.com/cover.jpg")))
            // Everything below is optional.
            .ean("5901234123457")
            .taxRate(TaxRate.TAX_23)
            .invoiceType(InvoiceType.VAT_INVOICE)
            .build());

    client.products().create(ProductExternalId.of("sku-1"), draft);
}
```

**Creation is asynchronous.** Erli answers `202 Accepted`; the product materialises in stages — `get`
starts answering shortly after, the images finish once the marketplace has downloaded and re-hosted
them, and `search` indexes it later still. Do not assert on a product the instant `create` returns;
poll `get` until it reports what you need.

Prices are `Money`. Erli states them as integer minor units ("w groszach") on the wire and the SDK
converts both ways, refusing an amount it cannot express exactly rather than rounding a penny away.

## Reading one product

```java
Optional<Product> found = client.products().get(ProductExternalId.of("sku-1"));
```

Absent means absent: a `404` comes back as an empty `Optional`, not an exception. Every other error
still throws.

A full product is a large payload. Ask for less when you need less:

```java
Optional<Product> summary = client.products()
        .get(ProductExternalId.of("sku-1"), Set.of(ProductField.NAME, ProductField.PRICE, ProductField.STOCK));
```

Unselected fields come back empty, so select everything you intend to read. The SDK quietly widens any
projection with the handful of scalars a `Product` cannot be built without (`externalId`, `name`,
`price`, `slug`, `created`, …) — the expensive parts, like the description, attributes, translations and
images, stay excluded unless you ask for them.

`Product` keeps three kinds of field distinguishable: what you authored, what the marketplace resolved
from it (`attributes()`, `categories()`, `translations()`, `slug()`), and lifecycle state
(`marketplaceId()`, `created()`, `buyableProblems()`). `isBuyable()` answers the common question, and
`buyableProblems()` says why not when it is false.

## Updating: omit, set, clear

A patch has three intentions per field, and Erli distinguishes all three. Getting this wrong is how a
partial update silently wipes data, so the SDK keeps them apart all the way to the wire:

| Intention | How | On the wire |
|---|---|---|
| leave alone | do not set it on the `ProductContent` | omitted |
| set | set it | the value |
| clear | name it in `clear(...)` | explicit `null` |

```java
ProductUpdateResult result = client.products().update(ProductExternalId.of("sku-1"),
        ProductPatch.builder()
                .content(ProductContent.builder().stock(5).build())   // set stock
                .clear(ProductField.MOBILE_PRICE)                     // remove the mobile price
                .build());

if (!result.changed(ProductField.STOCK)) {
    // The marketplace ignores writes to frozen fields — worth checking rather than assuming.
}
```

Only fields Erli marks nullable can be cleared; `clear(ProductField.NAME)` is rejected at the call
site with the field named. `ProductPatch.clearableFields()` is the full list.

Fields the seller froze are not overwritten by an ordinary update. Pass `.overrideFrozen(true)` to
write them anyway.

### Renaming

`.newExternalId(...)` changes the key. Every later call must use the new one.

### Many at once

```java
List<BatchUpdateOutcome> outcomes = client.products().updateAll(Map.of(
        ProductExternalId.of("sku-1"), restock(5),
        ProductExternalId.of("sku-2"), restock(0)));
```

The call succeeds as a whole while individual products fail, so results come back per product rather
than as an exception — check `isAccepted()` on each and read `error()` on the rest.

## Searching

`search` returns a **lazy** `Stream<Product>`: pages are fetched as you consume it, so a `limit` or a
short-circuit only costs the pages actually needed.

```java
try (Stream<Product> active = client.products().search(ProductSearchRequest.builder()
        .filter(ProductFilter.and(
                ProductFilter.equalTo(ProductFilterField.STATUS, "active"),
                ProductFilter.greaterThan(ProductFilterField.STOCK, "0")))
        .fields(Set.of(ProductField.NAME, ProductField.STOCK))
        .build())) {

    active.limit(100).forEach(this::reindex);
}
```

Filters compose to any depth with `and` / `or` / `not`, and leaves are comparisons or membership tests.
Field and operator are checked against each other when the filter is built, so an ordered comparison on
an equality-only field such as `STATUS` fails immediately with the field named, not as a 400.

**Walking the whole catalog requires a unique sort.** Product search is the one `_search` in the API
that returns a bare array with no cursor in the response, so the SDK derives the next page's cursor from
the sort field of the last row — and Erli treats that cursor as a *strict* bound. If several products
share the last row's value and did not fit on the page, the next page would begin past all of them and
those products would never be returned.

Only `EXTERNAL_ID` (the default) and `MARKETPLACE_ID` are unique per product. Sorting by anything else —
`UPDATED`, `NAME`, `EAN` — always returns the first page, but the stream throws `IllegalStateException`
if you ask it to continue, rather than paging on and silently returning an incomplete answer. (In the
one case where the last row has no value for the sort field at all — say `ARCHIVED_AT` over products
that were never archived — there is no cursor to page from, so the stream simply ends after that page.)
Note `updateAll` stamps the same `updated` on every product it touches, so that field ties readily.

```java
// Fine: the first page only, any sort. `limit` at or below the page size never asks for page two.
client.products().search(ProductSearchRequest.builder()
        .sortBy(ProductSortField.UPDATED, SortOrder.DESC)
        .pageSize(50)
        .build()).limit(50).toList();

// Fine: the whole catalog, unique sort.
client.products().search(ProductSearchRequest.all()).forEach(this::reindex);
```

## Timed promotions

```java
Discount discount = client.products().startDiscount(ProductExternalId.of("sku-1"),
        DiscountRequest.between(Money.ofPln("79.00"), startAt, restoreAt));

Optional<Discount> current = client.products().getDiscount(ProductExternalId.of("sku-1"));
```

The marketplace freezes the price for the promotion's duration. `unfreezeAfterwards` decides what
happens at `restoreAt`: `true` returns the product to normal seller-driven pricing, `false` leaves the
restored price frozen. `DiscountRequest.between(...)` uses `true`; the full constructor takes it
explicitly.

## Attribute values

Seller-supplied attributes carry values in one of four shapes, so `AttributeValues` is a sealed type
rather than a free-form blob — the compiler makes you handle each, and an unmatched shape cannot be
built by mistake.

```java
AttributeValues values = attribute.values();
if (values instanceof AttributeValues.TextValues text) {
    render(text.texts());
} else if (values instanceof AttributeValues.RangeValues range) {
    render(range.from(), range.to());
} else if (values instanceof AttributeValues.NumericValues numeric) {
    render(numeric.numbers());
} else if (values instanceof AttributeValues.DictionaryValues dictionary) {
    render(dictionary.entries());
}
```

## Errors

Exceptions group by what you can do about them, not by status code:

| Situation | Exception |
|---|---|
| bad key, no permission | `ErliAuthException` |
| no such product | `ErliNotFoundException` — except `get`/`getDiscount`, which return empty |
| rejected payload, or a product that already exists | `ErliValidationException` |
| marketplace fault, rate limit | `ErliServerException` |

Every one carries `details()` with `traceId`/`spanId` and the Polish message when the marketplace sent
one — quote the trace id when raising a ticket.
