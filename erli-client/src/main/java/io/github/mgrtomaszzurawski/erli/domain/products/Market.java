package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A marketplace a product is published to.
 */
public enum Market {

    /** The Polish marketplace. */
    PL("pl"),

    /** The German marketplace. */
    DE("de");

    private final String wireName;

    Market(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
