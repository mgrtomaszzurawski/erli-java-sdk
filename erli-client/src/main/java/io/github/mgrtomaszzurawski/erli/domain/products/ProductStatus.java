package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * Whether a product is offered for sale on the marketplace.
 */
public enum ProductStatus {

    /** The product is listed and buyable. */
    ACTIVE("active"),

    /** The product is withheld from sale. */
    INACTIVE("inactive");

    private final String wireName;

    ProductStatus(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
