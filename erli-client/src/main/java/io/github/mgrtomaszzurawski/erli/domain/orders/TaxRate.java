package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * The VAT rate that applied to an order item at the time of purchase. Erli's own identifiers are kept
 * (rather than being converted to percentages) because two of them are not rates at all:
 * {@link #TAX_NP} and {@link #TAX_ZW}.
 */
public enum TaxRate {

    /** 0%. */
    TAX_0,

    /** 5%. */
    TAX_5,

    /** 7%. */
    TAX_7,

    /** 8%. */
    TAX_8,

    /** 19%. */
    TAX_19,

    /** 23%. */
    TAX_23,

    /** {@code nie przysługuje} — VAT does not apply. */
    TAX_NP,

    /** {@code zwolniony} — exempt from VAT. */
    TAX_ZW
}
