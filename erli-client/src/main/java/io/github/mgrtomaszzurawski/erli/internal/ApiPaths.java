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
    /** {@code PATCH /products/batch-update} — update many products in one call. */
    public static final String PRODUCTS_BATCH_UPDATE = "/products/batch-update";

    /** {@code POST /products/_search} — search the seller's catalog (body cursor). */
    public static final String PRODUCTS_SEARCH = "/products/_search";

    /** {@code GET|POST|PATCH /products/{externalId}} — read, create or update one product. */
    public static final String PRODUCT_BY_EXTERNAL_ID = "/products/{externalId}";

    /** {@code GET|POST /products/{externalId}/discount} — read or start a timed discount. */
    public static final String PRODUCT_DISCOUNT = "/products/{externalId}/discount";

    // --- APPEND BLOCK: bucket B Orders ------------------------------------------------------------

    // --- APPEND BLOCK: bucket C Shipping & Delivery -----------------------------------------------

    // --- APPEND BLOCK: bucket D Dictionaries ------------------------------------------------------

    // --- APPEND BLOCK: bucket E Finance -----------------------------------------------------------

    // --- APPEND BLOCK: bucket F Comms & Automation ------------------------------------------------
}
