package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The market a product's own content is authored for; translations cover the others.
 */
public enum BaseMarket {

    /** The Polish marketplace. */
    PL("pl"),

    /** The German marketplace. */
    DE("de");

    private final String wireName;

    BaseMarket(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
