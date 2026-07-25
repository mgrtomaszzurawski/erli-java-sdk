package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * The Polish VAT rate applied to a product or order line. Erli's own identifiers are kept rather than
 * being converted to percentages, because two of them are not rates at all: {@link #TAX_NP}
 * ({@code nie podlega} — not subject to VAT) and {@link #TAX_ZW} ({@code zwolniony} — exempt).
 *
 * <p>Each constant carries its {@link #wireName()} — the exact token Erli uses on the wire — so a
 * mapper can round-trip a raw string without coupling the public constant name to the transport value.
 */
public enum TaxRate {

    /** 0%. */
    TAX_0("TAX_0"),

    /** 5%. */
    TAX_5("TAX_5"),

    /** 7%. */
    TAX_7("TAX_7"),

    /** 8%. */
    TAX_8("TAX_8"),

    /** 19%. */
    TAX_19("TAX_19"),

    /** 23%. */
    TAX_23("TAX_23"),

    /** Not subject to VAT ({@code nie podlega}). */
    TAX_NP("TAX_NP"),

    /** Exempt from VAT ({@code zwolniony}). */
    TAX_ZW("TAX_ZW");

    private final String wireName;

    TaxRate(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
