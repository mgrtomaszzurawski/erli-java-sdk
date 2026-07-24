# API surface — upstream operations and planned SDK mapping

Source: `openapi/swagger.json` (OpenAPI 3.0.0, 54 paths, 77 schemas). This maps upstream operations
to planned domain buckets. Deprecated operations are surfaced as current replacements only.

## Domains and operations

### products
`POST /products/_search` · `GET/POST/PATCH /products/{externalId}` · `PATCH /products/batch-update`
· `POST/GET /products/{externalId}/discount`

### orders
`POST /orders/_search` · `GET/PATCH /orders/{id}` · `PATCH /orders/{id}/status`

### inbox (messages)
`GET /inbox` · `POST /inbox/_search` · `POST /inbox/mark-read`

### payments
`POST /payments/operations/_search` · `GET /payments/operations/{id}` ·
`GET /payments/payouts/{id}` · (deprecated: `GET /payments/{id}`, `POST /payments/_search`,
`POST /payments/payouts/_search`)

### shipping
`POST /shipping/parcels/` · `POST /shipping/parcels/_search` · `GET/DELETE /shipping/parcels/{id}`
· `POST /shipping/external` · `GET/DELETE/PATCH /shipping/external/{id}` ·
`GET /shipping/pickupProtocols` · `GET /shipping/postingPoints`

### delivery
`GET /delivery/priceLists` · `GET /delivery/priceListsDetails` · `POST /delivery/priceList` ·
`PATCH /delivery/priceList/{id}`

### dictionaries
`POST /dictionaries/category/_search` (deprecated `categories/_search`) ·
`POST /dictionaries/attributes/_search` · `POST /dictionaries/attributeValues/_search` ·
`GET /dictionaries/deliveryMethods[/{priceList}]` · `GET /dictionaries/shippingMethods` ·
`GET /dictionaries/deliveryVendors` · `GET /dictionaries/billingEntryTypes` ·
`GET/POST/PATCH/DELETE /dictionaries/responsiblePersons[/{id}]` ·
`GET/POST/PATCH/DELETE /dictionaries/responsibleProducers[/{id}]` ·
`GET/DELETE /dictionaries/attachments` · `POST/PATCH /dictionaries/attachment` ·
`PATCH /dictionaries/attachment/attach|detach`

### billing
`POST /billing/entries` · `POST /billing/company/entries` · `POST /billing/company/rebates`

### commissions
`POST /commissions/_estimate`

### campaigns
`GET /campaigns/campaigns-summary`

### hooks (webhooks)
`GET /hooks` · `PUT/DELETE /hooks/{hookName}` · `POST /hooks/checkBuyability/run` ·
`POST /hooks/productsNeedSync/run`

### account
`GET /me`

## Conventions the surface must respect

- All list/search operations are cursor-paginated via the request body (`pagination.after`) →
  lazy `Stream<T>`.
- Writes are POST/PATCH; verify request bodies with WireMock on every write and retry test.
- Surface current operations only; keep deprecated paths out of the public API (or `@Deprecated`
  with a pointer to the replacement).

## Bucket fan-out (later)

The domain fan-out sizing (which worker owns which bucket) is decided once `sdk.core` and the
client module land; this document is the operation inventory it will draw from.
