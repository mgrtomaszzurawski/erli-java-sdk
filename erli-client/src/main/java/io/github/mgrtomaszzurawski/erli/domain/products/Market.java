package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A marketplace a product is published to.
 *
 * <p>Erli adds markets as it expands, so a payload can name one this SDK version does not know. That is
 * handled without widening this enum: see {@link ProductAttachment#unrecognisedMarkets()}, which keeps
 * the raw value so nothing is lost and the product can still be written back.
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
