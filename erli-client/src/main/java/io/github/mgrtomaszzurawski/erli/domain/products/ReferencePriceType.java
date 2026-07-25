package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * Which price the marketplace strikes through as the reference (\"was\") price.
 */
public enum ReferencePriceType {

    /** The marketplace-computed reference price (read-only; never sent on write). */
    MARKETPLACE_REFERENCE_PRICE("marketplaceReferencePrice"),

    /** The seller-declared catalogue price. */
    CATALOGUE_PRICE("cataloguePrice");

    private final String wireName;

    ReferencePriceType(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
