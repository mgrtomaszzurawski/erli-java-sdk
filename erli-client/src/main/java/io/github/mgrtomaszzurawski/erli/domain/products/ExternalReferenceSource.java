package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The channel that produced an external product reference.
 */
public enum ExternalReferenceSource {

    /** Discovered by the marketplace scrapper. */
    SCRAPPER("scrapper"),

    /** Supplied by ProfitWatch. */
    PROFITWATCH("profitwatch"),

    /** Supplied through the public API. */
    API("api"),

    /** Entered by hand in the shop panel. */
    MANUAL("manual"),

    /** Derived locally by the marketplace. */
    LOCAL("local"),

    /** Supplied by an RSS integration. */
    INTEGRATION_RSS("integration-rss");

    private final String wireName;

    ExternalReferenceSource(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
