# Shipping

Hand parcels to Erli's carrier integration, or register parcels you shipped yourself, through
`client.shipping()`. Ten operations are covered: `POST /shipping/parcels/`,
`GET` and `DELETE` on `/shipping/parcels/{id}`, `POST /shipping/parcels/_search`, the four
`/shipping/external` operations, `GET /shipping/pickupProtocols` and `GET /shipping/postingPoints`.

Delivery *pricing* is a separate area — see [`docs/delivery.md`](delivery.md).

```java
try (ErliClient client = ErliClient.fromEnvironment()) {
    Parcel parcel = client.shipping().parcel(ParcelId.of("55123"));
    System.out.println(parcel.status() + " " + parcel.trackingNumber().orElse("no tracking yet"));
}
```

## Two kinds of parcel

Erli models them as separate resources and so does the SDK:

| | Erli-carried | Externally shipped |
|---|---|---|
| Created with | `createParcels` | `registerExternalParcels` |
| Read as | `Parcel` | `ExternalParcel` |
| Carries | dimensions, posting point, sender/receiver, waybills | carrier, tracking number, status |
| Erli does | books the carrier, issues the label | tracks what you tell it |

`ExternalParcel` is deliberately thinner: Erli never handled the package, so there is no waybill to
issue and no posting point to hand it in at.

## Creating parcels

`createParcels` is **all-or-nothing** — the API refuses the whole batch if any order is cancelled, or
if a second parcel for an order gives a receiver address that differs from the first. When that
happens the validation error names the differing fields.

```java
ShippingParty receiver = new ShippingParty(
        Optional.of("Jan"), Optional.of("Nowak"), Optional.empty(),
        Optional.of("Kwiatowa"), Optional.of("7"), Optional.empty(),
        Optional.of("Warszawa"), Optional.of("00-950"), Optional.of(ShippingCountry.PL),
        Optional.of("600300400"), Optional.of("buyer@example.com"),
        Optional.of(PickupType.COURIER), Optional.empty());

List<Parcel> created = client.shipping().createParcels(List.of(
        ParcelDraft.builder(
                        OrderId.of("221201x12345"),
                        ShippingMethodId.of("erliKurier24InPost10kg"),
                        new ParcelDimensions(
                                new BigDecimal("200"), new BigDecimal("100"), new BigDecimal("300"), 1500),
                        receiver)
                .additionalInformation("Leave at reception")
                .build()));
```

### Dimensions are millimetres and grams

`ParcelDimensions` takes **millimetres**, not centimetres, and the weight in **grams** — the spec bounds
each edge to 1–2000 mm and the weight to 10–700 000 g. A 20 × 10 × 30 cm parcel is
`(200, 100, 300)`. The edges are `BigDecimal` so the value you send is the value the carrier prices;
nothing is rounded on the way out.

## Searching

`searchParcels` takes **one** filter, not a list — Erli's search body carries a single filter object,
with no array and no and/or wrapper. `ParcelFilter`'s factory methods pin the operator, so a comparison
always carries one value and a membership test always carries a list; the invalid pairings cannot be
constructed.

```java
List<Parcel> forOrder = client.shipping()
        .searchParcels(ParcelFilter.isEqualTo(ParcelSearchField.ORDER_ID, "221201x12345"));

List<Parcel> byIds = client.shipping()
        .searchParcels(ParcelFilter.isAnyOf(ParcelSearchField.ID, List.of("55123", "55124")));
```

Filterable fields are `ID`, `ORDER_ID` and `UPDATED_AT`. Unlike orders, this endpoint returns a plain
list rather than a cursor page.

## Cancelling

`cancelParcel` returns the parcel in its cancelled state. If a courier pickup is already booked the API
refuses — cancel the pickup first, then the parcel.

## Externally shipped parcels

`registerExternalParcels` reports **per entry**: Erli validates each row independently, so one row it
refuses does not discard the rows it accepted. The result is a sealed type, which is why you cannot
accidentally read a parcel that was never created.

One rule is still call-wide, though: like `createParcels`, this endpoint refuses the **whole** call if
any referenced order is cancelled. Per-entry results are what you get once the call itself is accepted.

```java
for (ExternalParcelResult result : client.shipping().registerExternalParcels(drafts)) {
    if (result instanceof ExternalParcelResult.Created created) {
        store(created.parcel().id());
    } else if (result instanceof ExternalParcelResult.Rejected rejected) {
        log.warn("order {} refused: {}", rejected.orderId(), rejected.errors());
    }
}
```

`status` is only accepted for carriers Erli cannot track itself (own transport, self pickup, the pallet
forwarders); for the tracked ones Erli derives it. Passing a status the endpoint cannot set is rejected
locally, before the request is made, naming the status.

## Parcel status is tolerant on purpose

`ParcelStatus` has an `UNRECOGNIZED` constant. Statuses come from carrier integrations and grow as Erli
adds carriers, so an unknown value degrades to that constant rather than failing the read — one new
status must not cost you every other parcel in the same search response.

Two consequences worth knowing:

- **`UNRECOGNIZED` does not carry the string Erli sent.** The SDK exposes no raw payload on a successful
  read, so that value is lost. If you need it, that is a gap worth raising.
- **An absent status also reads as `UNRECOGNIZED`.** After decoding, a missing field and an unknown
  value are indistinguishable, so the SDK cannot tell them apart either.

The other wire-mapped enums (`ParcelType`, `PickupType`, `ShippingCountry`, `PostingPointType`) keep
the opposite contract: there an unknown value means something is genuinely wrong, so it throws.
`ParcelSearchField` and `ParcelSearchOperator` are request-only and never decode anything.

## Buyer personal data

`ParcelShipment.receiver()` carries the buyer's name, delivery address, postcode, phone number and
e-mail. `toString()` on every record that touches it is hand-written to disclose presence without
content, so a parcel that reaches a log line or a stack trace does not leak buyer data:

```
ShippingParty[firstName=***, lastName=***, companyName=null, street=***, buildingNumber=***,
              flatNumber=null, city=***, zip=***, country=pl, phoneNumber=***, email=***,
              pickupType=point, pointCode=WAW01A]
```

Every component is listed: `***` means present-but-withheld, `null` means genuinely absent — so the
output still tells you whether a field arrived.

Redaction also covers two things that are not obviously personal: `additionalInformation`, the free-text
courier note, which routinely restates the address; and the `waybills` / `pickupProtocol` links, which
retrieve documents containing the buyer's name and address — only the waybill count is disclosed.

Call the accessors to obtain the real values. `ParcelDraft` redacts the same way, so the outbound draft
does not leak what the inbound record protects.

## Pickup protocols and posting points

`pickupProtocols` returns **one** link covering every parcel you asked for, not one per parcel. Treat
the URL as if it were the document — it lists the parcels and their destinations.

```java
String url = client.shipping()
        .pickupProtocols(List.of(ParcelId.of("55123"), ParcelId.of("55124"))).url();
```

`postingPoints` describes where you hand parcels over. `PostingPoint.type()` discriminates the three
shapes: `ADDRESS` (a courier collects from you), `POINT` (one drop-off, in `point()`), and `POINTS`
(a choice of several, in `points()`). The contact details on a posting point are your own site's, not a
buyer's, so they are not redacted.

## Errors

| HTTP | Exception |
|---|---|
| 401 / 403 | `ErliAuthException` |
| 404 | `ErliNotFoundException` |
| 400 / 409 / 422 | `ErliValidationException` |
| 5xx | `ErliServerException` |

Errors that arrive *inside* a successful response are different: a parcel can come back `200` with
`status = ERROR` and the reason in `errors()`. Check that list rather than assuming a `200` means the
carrier accepted the shipment.

Bad input is rejected before any request is made and raises `IllegalArgumentException`, not an
`Erli*` exception — no API call happened. That covers an empty batch, an unknown delivery method, a
status the external endpoint cannot set, and a parcel id of `.` or `..` (which would otherwise collapse
to the collection endpoint and, on a delete, act on far more than you meant).

## Optionality

Fields the API marks required are non-`Optional` and throw with the field name if a response omits
them. Everything else is `Optional`. `Parcel.id()` is `Optional` because a parcel only has an id once
Erli has accepted it — one that came back from a read always has one.
