package io.github.mgrtomaszzurawski.erli.internal;

/**
 * Request path constants, relative to the configured base URL (e.g. {@code /svc/shop-api}).
 *
 * <p>This is one of the three shared <em>append points</em> in the fan-out plan. Each domain bucket
 * adds its path constants under its own reserved block below so appends never collide; the core owner
 * pre-reserves one block per bucket. Keep constants grouped by bucket and alphabetical within a block.
 * Internal: never exported.
 */
public final class ApiPaths {

    private ApiPaths() {
    }

    // --- Core -------------------------------------------------------------------------------------
    /** {@code GET /me} — the authenticated shop (the Core M1 proof slice). */
    public static final String ME = "/me";

    // --- APPEND BLOCK: bucket A Products ----------------------------------------------------------

    // --- APPEND BLOCK: bucket B Orders ------------------------------------------------------------
    /** {@code GET /orders/{id}} — one order; also {@code PATCH} for a partial update. */
    public static final String ORDER_BY_ID = "/orders/{id}";

    /** {@code PATCH /orders/{id}/status} — move the order's seller-side status. */
    public static final String ORDER_STATUS = "/orders/{id}/status";

    /** {@code POST /orders/_search} — the paged order search. */
    public static final String ORDERS_SEARCH = "/orders/_search";

    // --- APPEND BLOCK: bucket C Shipping & Delivery -----------------------------------------------
    /** {@code GET|DELETE /shipping/parcels/{id}} — one parcel by id, or cancel it. */
    public static final String SHIPPING_PARCEL_BY_ID = "/shipping/parcels/{id}";
    /** {@code POST /shipping/parcels/} — hand parcels to Erli's carrier integration. */
    public static final String SHIPPING_PARCELS = "/shipping/parcels/";
    /** {@code POST /shipping/parcels/_search} — search parcels. */
    public static final String SHIPPING_PARCELS_SEARCH = "/shipping/parcels/_search";
    /** {@code POST /shipping/external} — register externally shipped parcels. */
    public static final String SHIPPING_EXTERNAL = "/shipping/external";
    /** {@code GET|PATCH|DELETE /shipping/external/{id}} — one external parcel by id. */
    public static final String SHIPPING_EXTERNAL_BY_ID = "/shipping/external/{id}";
    /** {@code GET /shipping/pickupProtocols} — courier pickup confirmations for a set of parcels. */
    public static final String SHIPPING_PICKUP_PROTOCOLS = "/shipping/pickupProtocols";
    /** {@code GET /shipping/postingPoints} — the shop's defined posting points. */
    public static final String SHIPPING_POSTING_POINTS = "/shipping/postingPoints";
    /** {@code POST /delivery/priceList} — create a delivery price list. */
    public static final String DELIVERY_PRICE_LIST = "/delivery/priceList";
    /** {@code PATCH /delivery/priceList/{id}} — replace a price list's content. */
    public static final String DELIVERY_PRICE_LIST_BY_ID = "/delivery/priceList/{id}";
    /** {@code GET /delivery/priceLists} — price lists, id and name only. */
    public static final String DELIVERY_PRICE_LISTS = "/delivery/priceLists";
    /** {@code GET /delivery/priceListsDetails} — price lists with their priced entries. */
    public static final String DELIVERY_PRICE_LISTS_DETAILS = "/delivery/priceListsDetails";

    // --- APPEND BLOCK: bucket D Dictionaries ------------------------------------------------------
    /** {@code GET /dictionaries/deliveryMethods} — delivery methods reference list. */
    public static final String DICTIONARIES_DELIVERY_METHODS = "/dictionaries/deliveryMethods";

    // --- APPEND BLOCK: bucket E Finance -----------------------------------------------------------
    /** {@code POST /billing/company/entries} — settlement history for the whole company. */
    public static final String BILLING_COMPANY_ENTRIES = "/billing/company/entries";
    /** {@code POST /billing/company/rebates} — rebate reserve and history for the whole company. */
    public static final String BILLING_COMPANY_REBATES = "/billing/company/rebates";
    /** {@code GET /campaigns/campaigns-summary} — daily ad-campaign cost summary for the shop. */
    public static final String CAMPAIGNS_SUMMARY = "/campaigns/campaigns-summary";
    /** {@code POST /commissions/_estimate} — commission estimated for the current day. */
    public static final String COMMISSIONS_ESTIMATE = "/commissions/_estimate";
    /** {@code GET /payments/operations/{id}} — one payment or payout operation. */
    public static final String PAYMENT_OPERATION_BY_ID = "/payments/operations/{id}";
    /** {@code POST /payments/operations/_search} — search payments, payouts or returns. */
    public static final String PAYMENT_OPERATIONS_SEARCH = "/payments/operations/_search";

    // --- APPEND BLOCK: bucket F Comms & Automation ------------------------------------------------
    /** Name of the {@link #HOOK_BY_NAME} placeholder, as {@link PathTemplate#expand} expects it. */
    public static final String HOOK_NAME_PARAM = "hookName";

    /** {@code POST /hooks/checkBuyability/run} — test-fire the shop's buyability hook. */
    public static final String HOOK_CHECK_BUYABILITY_RUN = "/hooks/checkBuyability/run";
    /** {@code POST /hooks/productsNeedSync/run} — test-fire the shop's product-sync hook. */
    public static final String HOOK_PRODUCTS_NEED_SYNC_RUN = "/hooks/productsNeedSync/run";
    /** {@code GET /hooks} — the shop's registered webhook subscriptions. */
    public static final String HOOKS = "/hooks";
    /** {@code PUT}/{@code DELETE /hooks/{hookName}} — save or remove one subscription. */
    public static final String HOOK_BY_NAME = "/hooks/{hookName}";
    /** {@code GET /inbox} — the 500 oldest unread messages. */
    public static final String INBOX = "/inbox";
    /** {@code POST /inbox/mark-read} — acknowledge messages as read. */
    public static final String INBOX_MARK_READ = "/inbox/mark-read";
    /** {@code POST /inbox/_search} — the 500 oldest unread messages, filtered by type. */
    public static final String INBOX_SEARCH = "/inbox/_search";
}
