# Delivery pricing

Manage the shop's delivery price lists through `client.delivery()` — which methods you offer and what
each costs. Four operations are covered: `GET /delivery/priceLists`, `GET /delivery/priceListsDetails`,
`POST /delivery/priceList` and `PATCH /delivery/priceList/{id}`.

Parcels and their carriage are a separate area — see [`docs/shipping.md`](shipping.md).

```java
try (ErliClient client = ErliClient.fromEnvironment()) {
    for (PriceListSummary summary : client.delivery().priceLists()) {
        System.out.println(summary.id() + " " + summary.name());
    }
}
```

## Two read endpoints

`priceLists()` is the index — id and name only. `priceListDetails(query)` returns the same lists with
their priced entries, and takes filters:

```java
List<PriceList> lists = client.delivery().priceListDetails(
        PriceListQuery.builder()
                .name("heavy")
                .erliProEnabled(true)
                .build());
```

Ids and names accumulate; the API treats repeats as "any of". `PriceListQuery.none()` fetches
everything.

A shop can keep several lists and attach different ones to different products. The default list is
named `"*"` — but do not assume one exists: a fresh shop can have **no price lists at all**, and both
endpoints then return an empty list.

## Money

Both amounts are `Money`, never raw integers. Erli states them in grosze on the wire; the SDK converts
in both directions so you never have to remember the scale.

```java
DeliveryPrice price = new DeliveryPrice(
        new DeliveryMethodRef(
                DeliveryMethodId.of("erliPaczkomat"),
                Optional.of(new DeliveryTime(DeliveryTimeUnit.DAYS, 1, 2))),
        Money.ofMinorUnits(1049, "PLN"),   // 10.49 zł — cost of the first item
        Money.ofMinorUnits(800, "PLN"),    // 8.00 zł — surcharge per additional item
        Optional.of(new PackingLimit.Total(10)),
        false);
```

`nextItemPrice` is the surcharge for each *additional* item in the same parcel, not a total.

**Delivery is priced in PLN only.** The payloads carry no currency field in either direction, so the
SDK decodes grosze unconditionally and refuses to send anything else rather than reinterpreting, say,
yen as grosze. A non-PLN amount raises `IllegalArgumentException` before the request is made, as does
an amount that is not a whole number of grosze.

## Packing limits

How many items fit in one parcel is a `oneOf` on the wire — a plain count for most methods, or a
per-bracket table for `erliPaczkomat`, where an InPost locker's A/B/C compartments hold different
amounts. The SDK re-types it as a sealed pair, so you pattern-match instead of inspecting a raw map:

```java
PackingLimit limit = price.limit().orElseThrow();
if (limit instanceof PackingLimit.Total total) {
    System.out.println("up to " + total.maxItems() + " items");
} else if (limit instanceof PackingLimit.PerSize perSize) {
    perSize.limits().forEach(row -> System.out.println(row.dimension() + ": " + row.limit()));
}
```

The API requires all three brackets when the method is `erliPaczkomat`.

## Creating and replacing

```java
PriceList created = client.delivery().createPriceList(
        PriceListDraft.builder("heavy")
                .price(price)
                .erliProEnabled(true)
                .build());
```

Names are unique per shop; reusing one is refused with a validation error.

`updatePriceList` is a **replacement, not a merge**. The API requires the full price set on every
update, so a delivery method you omit is removed from the list. Read the current list, modify, and send
it back in full:

```java
PriceList current = client.delivery().priceListDetails(
        PriceListQuery.builder().id(42).build()).get(0);

PriceListUpdate.Builder update = PriceListUpdate.builder()
        .erliProEnabled(current.erliProEnabled())
        .nextDayDeliveryEnabled(current.nextDayDeliveryEnabled());
current.prices().forEach(update::price);          // keep what you are not changing
client.delivery().updatePriceList(current.id(), update.build());
```

The list's name cannot be changed this way.

## Delivery methods

`DeliveryMethodRef.id()` is the core `DeliveryMethodId`, not an SDK enum — the spec lists over a hundred
method ids and grows with every carrier deal, and `client.dictionaries().deliveryMethods()` already
exposes the authoritative list keyed by the same id.

An id this release does not know is rejected locally, before the request, with a message naming the
value and pointing at that dictionary. That keeps a typo from becoming a server-side 400, but it does
mean a method Erli adds after this SDK version needs an SDK update before you can price it.

## ErliPRO

`erliProEnabled` opts the products on a list into the free-delivery programme. The API caps the base
price per method for lists that join it — `erliPaczkomat` at 10.49 zł, `erliKurier24InPost10kg` at
12.69 zł, and so on — and requires at least one ERLI InPost method to be enabled. Exceeding a cap is
answered with a validation error naming the method.

## Errors

| HTTP | Exception |
|---|---|
| 401 / 403 | `ErliAuthException` |
| 404 | `ErliNotFoundException` |
| 400 / 409 / 422 | `ErliValidationException` |
| 5xx | `ErliServerException` |

Note this area does **not** answer a missing price list with a 404: patching an id that does not exist
comes back as a validation error. Do not branch on `ErliNotFoundException` here expecting to catch it.

Bad input raises `IllegalArgumentException` rather than an `Erli*` exception, because no request was
made — an empty price set, a non-PLN or fractional amount, or an unknown delivery method.

## Optionality

`PriceList.prices()` is never empty; the API requires at least one entry, and `PriceListDraft` and
`PriceListUpdate` enforce that locally too. `updatedAt` is `Optional` — a list that has never been
changed does not have one.
