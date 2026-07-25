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

    // --- APPEND BLOCK: bucket C Shipping & Delivery -----------------------------------------------

    // --- APPEND BLOCK: bucket D Dictionaries ------------------------------------------------------
    /** {@code POST /dictionaries/attributes/_search} — attributes defined for a category. */
    public static final String DICTIONARIES_ATTRIBUTES_SEARCH = "/dictionaries/attributes/_search";
    /** {@code POST /dictionaries/attributeValues/_search} — allowed values of dictionary attributes. */
    public static final String DICTIONARIES_ATTRIBUTE_VALUES_SEARCH = "/dictionaries/attributeValues/_search";
    /** {@code GET /dictionaries/billingEntryTypes} — billing operation types. */
    public static final String DICTIONARIES_BILLING_ENTRY_TYPES = "/dictionaries/billingEntryTypes";
    /** {@code POST /dictionaries/category/_search} — the marketplace category tree. */
    public static final String DICTIONARIES_CATEGORY_SEARCH = "/dictionaries/category/_search";
    /** {@code GET /dictionaries/deliveryMethods} — delivery methods that may appear on an order. */
    public static final String DICTIONARIES_DELIVERY_METHODS = "/dictionaries/deliveryMethods";
    /** {@code GET /dictionaries/deliveryMethods/{priceList}} — delivery methods for one price list. */
    public static final String DICTIONARIES_DELIVERY_METHODS_BY_PRICE_LIST =
            "/dictionaries/deliveryMethods/{priceList}";
    /** {@code GET /dictionaries/deliveryVendors} — carriers ERLI accepts tracking numbers for. */
    public static final String DICTIONARIES_DELIVERY_VENDORS = "/dictionaries/deliveryVendors";
    /** {@code GET /dictionaries/responsiblePersons} — persons who introduced a product to the market. */
    public static final String DICTIONARIES_RESPONSIBLE_PERSONS = "/dictionaries/responsiblePersons";
    /** {@code GET /dictionaries/responsibleProducers} — product producers. */
    public static final String DICTIONARIES_RESPONSIBLE_PRODUCERS = "/dictionaries/responsibleProducers";
    /** {@code GET /dictionaries/shippingMethods} — ERLI's own shipping methods. */
    public static final String DICTIONARIES_SHIPPING_METHODS = "/dictionaries/shippingMethods";

    // --- APPEND BLOCK: bucket E Finance -----------------------------------------------------------

    // --- APPEND BLOCK: bucket F Comms & Automation ------------------------------------------------
}
