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
    /** {@code GET /shipping/parcels/{id}} — one parcel by id. */
    public static final String SHIPPING_PARCEL_BY_ID = "/shipping/parcels/{id}";

    // --- APPEND BLOCK: bucket D Dictionaries ------------------------------------------------------
    /** Placeholder substituted with the price-list name in {@link #DICTIONARIES_DELIVERY_METHODS_BY_PRICE_LIST}. */
    public static final String PRICE_LIST_PLACEHOLDER = "{priceList}";
    /** Placeholder substituted with the entry id in the responsible-party paths. */
    public static final String RESPONSIBLE_ID_PLACEHOLDER = "{id}";

    /** {@code POST}/{@code PATCH /dictionaries/attachment} — create or update one attachment. */
    public static final String DICTIONARIES_ATTACHMENT = "/dictionaries/attachment";
    /** {@code PATCH /dictionaries/attachment/attach} — attach products to an attachment. */
    public static final String DICTIONARIES_ATTACHMENT_ATTACH = "/dictionaries/attachment/attach";
    /** {@code PATCH /dictionaries/attachment/detach} — detach products from an attachment. */
    public static final String DICTIONARIES_ATTACHMENT_DETACH = "/dictionaries/attachment/detach";
    /** {@code GET}/{@code DELETE /dictionaries/attachments} — list or remove attachments. */
    public static final String DICTIONARIES_ATTACHMENTS = "/dictionaries/attachments";
    /** {@code POST /dictionaries/attributes/_search} — attributes defined for a category. */
    public static final String DICTIONARIES_ATTRIBUTES_SEARCH = "/dictionaries/attributes/_search";
    /** {@code POST /dictionaries/attributeValues/_search} — allowed values of dictionary attributes. */
    public static final String DICTIONARIES_ATTRIBUTE_VALUES_SEARCH = "/dictionaries/attributeValues/_search";
    /** {@code GET /dictionaries/billingEntryTypes} — billing operation types. */
    public static final String DICTIONARIES_BILLING_ENTRY_TYPES = "/dictionaries/billingEntryTypes";
    /** {@code POST /dictionaries/category/_search} — the marketplace category tree. */
    public static final String DICTIONARIES_CATEGORY_SEARCH = "/dictionaries/category/_search";
    /** {@code GET /dictionaries/deliveryMethods} — delivery methods reference list. */
    public static final String DICTIONARIES_DELIVERY_METHODS = "/dictionaries/deliveryMethods";
    /** {@code GET /dictionaries/deliveryMethods/{priceList}} — delivery methods for one price list. */
    public static final String DICTIONARIES_DELIVERY_METHODS_BY_PRICE_LIST =
            "/dictionaries/deliveryMethods/{priceList}";
    /** {@code GET /dictionaries/deliveryVendors} — carriers ERLI accepts tracking numbers for. */
    public static final String DICTIONARIES_DELIVERY_VENDORS = "/dictionaries/deliveryVendors";
    /** {@code GET}/{@code POST /dictionaries/responsiblePersons} — persons who introduced a product. */
    public static final String DICTIONARIES_RESPONSIBLE_PERSONS = "/dictionaries/responsiblePersons";
    /** {@code PATCH}/{@code DELETE /dictionaries/responsiblePersons/{id}} — update or remove one person. */
    public static final String DICTIONARIES_RESPONSIBLE_PERSON_BY_ID = "/dictionaries/responsiblePersons/{id}";
    /** {@code GET}/{@code POST /dictionaries/responsibleProducers} — product producers. */
    public static final String DICTIONARIES_RESPONSIBLE_PRODUCERS = "/dictionaries/responsibleProducers";
    /** {@code PATCH}/{@code DELETE /dictionaries/responsibleProducers/{id}} — update or remove one producer. */
    public static final String DICTIONARIES_RESPONSIBLE_PRODUCER_BY_ID = "/dictionaries/responsibleProducers/{id}";
    /** {@code GET /dictionaries/shippingMethods} — ERLI's own shipping methods. */
    public static final String DICTIONARIES_SHIPPING_METHODS = "/dictionaries/shippingMethods";

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
