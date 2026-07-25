package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The JSON type a {@link ProductFilterField}'s value takes on the wire.
 *
 * <p>Filter values are carried through the SDK as text, because one filter can compare a name, a price,
 * a timestamp or a flag. Erli, however, types the value per field and rejects a mismatch — a stock
 * comparison sent as {@code "0"} rather than {@code 0} does not match, and {@code archived} sent as
 * {@code "true"} is refused outright. This tells the SDK which conversion to apply, so callers never
 * have to think about it.
 */
public enum FilterValueKind {

    /** A JSON string, sent verbatim. */
    TEXT,

    /** A JSON number. */
    NUMBER,

    /** A JSON boolean. */
    BOOLEAN,

    /** An ISO-8601 timestamp, sent as a JSON string the marketplace parses as a date. */
    DATE_TIME
}
