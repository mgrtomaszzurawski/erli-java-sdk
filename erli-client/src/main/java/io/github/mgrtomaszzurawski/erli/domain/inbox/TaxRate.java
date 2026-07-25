package io.github.mgrtomaszzurawski.erli.domain.inbox;

/**
 * VAT rate that applied to an order line at purchase time. Mirrors the API enum, which mixes
 * percentages with the two non-numeric Polish cases.
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
    TAX_NOT_APPLICABLE,
    /** {@code zwolniony} — exempt from VAT. */
    TAX_EXEMPT
}
