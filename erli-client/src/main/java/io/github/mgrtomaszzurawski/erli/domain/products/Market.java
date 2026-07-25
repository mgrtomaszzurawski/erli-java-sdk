package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A marketplace a product is published to.
 */
public enum Market {

    /** The Polish marketplace. */
    PL("pl"),

    /** The German marketplace. */
    DE("de"),

    /**
     * A market this SDK version does not know.
     *
     * <p>Erli adds markets as it expands, and a product attachment carries its market scope as a list.
     * Rejecting one unfamiliar entry would fail the whole product read — and, on a search, the whole
     * page — over a value the caller very likely does not even care about. So an unknown market is
     * surfaced as this constant instead: the scope keeps its true size, and a caller that inspects it
     * can see there is a market it cannot name, which silently dropping the entry would have hidden.
     *
     * <p>It is a <strong>read-only</strong> constant. {@link #wireName()} refuses to render it, because
     * the SDK does not know what value it stood for and must not invent one; remove it from the list
     * before writing the product back, or upgrade the SDK.
     *
     * <p>Unreachable where the API declares the field as an enum rather than a list of strings — there
     * the transport decodes an unknown value to absent before the mapper ever sees it.
     */
    UNRECOGNIZED(null);

    private final String wireName;

    Market(String wireName) {
        this.wireName = wireName;
    }

    /**
     * The value the Erli API uses for this constant on the wire.
     *
     * @throws IllegalStateException for {@link #UNRECOGNIZED}, which has no known wire value
     */
    public String wireName() {
        if (wireName == null) {
            throw new IllegalStateException(
                    "Market." + name() + " stands for a market this SDK version does not know, so it"
                            + " cannot be written back. Remove it from the list before sending, or"
                            + " upgrade the SDK.");
        }
        return wireName;
    }
}
