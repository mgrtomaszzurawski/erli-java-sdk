package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * Whether a product is offered for sale on the marketplace.
 */
public enum ProductStatus {

    /** The product is listed and buyable. */
    ACTIVE,

    /** The product is withheld from sale. */
    INACTIVE
}
