package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * Where a product's external variant group originated.
 */
public enum VariantGroupSource {

    /** Grouped by the Erli marketplace. */
    MARKETPLACE("marketplace"),

    /** Grouped by an external integration. */
    INTEGRATION("integration");

    private final String wireName;

    VariantGroupSource(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
