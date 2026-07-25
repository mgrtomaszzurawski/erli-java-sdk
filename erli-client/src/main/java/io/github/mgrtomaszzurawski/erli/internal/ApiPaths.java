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
}
