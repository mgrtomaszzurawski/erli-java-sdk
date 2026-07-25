package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The Polish VAT rate applied to a product. {@code NP} = not subject to VAT, {@code ZW} = exempt.
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

    /** Not subject to VAT (\"nie podlega\"). */
    TAX_NP("TAX_NP"),

    /** Exempt from VAT (\"zwolniony\"). */
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
