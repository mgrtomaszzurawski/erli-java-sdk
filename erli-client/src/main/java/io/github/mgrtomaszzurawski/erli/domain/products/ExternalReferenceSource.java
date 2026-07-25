package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The channel that produced an external product reference.
 */
public enum ExternalReferenceSource {

    /** Discovered by the marketplace scrapper. */
    SCRAPPER,

    /** Supplied by ProfitWatch. */
    PROFITWATCH,

    /** Supplied through the public API. */
    API,

    /** Entered by hand in the shop panel. */
    MANUAL,

    /** Derived locally by the marketplace. */
    LOCAL,

    /** Supplied by an RSS integration. */
    INTEGRATION_RSS
}
