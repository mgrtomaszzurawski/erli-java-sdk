# Known server / spec behaviors

Observed facts about the upstream Erli Marketplace API and its OpenAPI spec that the SDK has to
work around. Each entry is a fact plus how the SDK handles it. Add a new entry when a live call or
a build step reveals behavior the naive reading of the spec would miss.

## Spec quality

- **Spec fails strict OpenAPI validation (6 issues).** Path parameters (`id`, `priceList`,
  `hookName`, `externalId`) omit `required: true`; `CheckBuyabilityResponse` carries a stray
  `examples`; the `GET /products/{externalId}` `fields` parameter carries a top-level `default`.
  These are non-fatal. The generator runs with `skipValidateSpec` (the vendored spec is
  source-of-truth and is never hand-edited).

- **A few polymorphic `oneOf`/`anyOf` "value" schemas contain an inline array branch**
  (`CreatePriceListSchema` price `limit`; the filter `value` fields of `OrderFilter`,
  `ProductFilter`, `SearchParcels`, `SearchPayments`; `ProductUpdate.overrideFrozen`;
  `ProductResponse` translation attribute values). openapi-generator emits an invalid
  `List<X>.class` token for these. A build-time normalization step (`normalizeSpec`, see
  `erli-rest-models/build.gradle.kts`) collapses exactly these array-branch composites to free-form
  objects (`Object` at Layer 1). The vendored spec is untouched; the domain layer re-types them.

- **Array-of-enum properties trip the generator's `toUrlQueryString` helper** (it iterates
  `for (String item : getFields())` over a `List<...Enum>`). The same normalization step drops the
  `enum` from array `items` so Layer 1 exposes `List<String>`; scalar enums are kept. Affected:
  `ProductSearch.fields`, `ProductUpdateResponse.updatedFields`, `ProductCreate/Update`/
  `PatchAttachmentRequest`/`AddAttachmentRequest` `markets`, `MessageRequest.types`,
  `SearchTransactions.types`, `ProductResponse.buyableProblems`, and the attachment `market(s)`.
  The `toUrlQueryString` helper itself is dead code for this SDK (transport is reimplemented).

## API semantics

- **Deprecations already live in the spec.** `GET /payments/{id}`, `POST /payments/_search`,
  `POST /payments/payouts/_search` → use `payments/operations/...`. `POST
  /dictionaries/categories/_search` → use `dictionaries/category/_search`. Order search
  `sortField=id` unsupported since 2025-03. The SDK surfaces current operations only.

- **Pagination is a body cursor.** `POST .../_search` takes `pagination.after`; list items carry a
  `cursor` (orders use `unixTimestamp;idN` to avoid equal-time skips). No `page`/`limit`/`offset`
  query params exist.

- **Errors use documented codes.** `1100` server, `1200` validation, `1300` auth, `1400`
  not-found; responses carry `traceId`/`spanId` and an optional `polishMessage`.

## To verify on the live/test environment (BOK-allocated creds required)

- Rate-limit headers and whether `Retry-After` is honored on 429.
- Whether `traceId`/`spanId` appear on all error responses.
- Actual cursor stability across pages for products vs. orders.
