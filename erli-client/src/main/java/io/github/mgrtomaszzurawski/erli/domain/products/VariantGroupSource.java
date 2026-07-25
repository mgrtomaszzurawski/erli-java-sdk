package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * Where a product's external variant group originated.
 */
public enum VariantGroupSource {

    /** Grouped by the Erli marketplace. */
    MARKETPLACE,

    /** Grouped by an external integration. */
    INTEGRATION
}
