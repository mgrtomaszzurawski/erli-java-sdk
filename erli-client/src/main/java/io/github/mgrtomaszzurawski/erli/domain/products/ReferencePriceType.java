package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * Which price the marketplace strikes through as the reference (\"was\") price.
 */
public enum ReferencePriceType {

    /** The marketplace-computed reference price (read-only; never sent on write). */
    MARKETPLACE_REFERENCE_PRICE,

    /** The seller-declared catalogue price. */
    CATALOGUE_PRICE
}
