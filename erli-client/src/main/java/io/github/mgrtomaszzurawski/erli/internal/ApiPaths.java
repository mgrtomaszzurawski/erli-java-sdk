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

    // --- APPEND BLOCK: bucket D Dictionaries ------------------------------------------------------

    // --- APPEND BLOCK: bucket E Finance -----------------------------------------------------------

    // --- APPEND BLOCK: bucket F Comms & Automation ------------------------------------------------
}
