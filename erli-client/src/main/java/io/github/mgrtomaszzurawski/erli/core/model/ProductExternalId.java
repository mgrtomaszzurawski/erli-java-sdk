package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * The seller-assigned external identifier of a product (Erli addresses products by
 * {@code externalId}, not an internal id). Owned by core; shared by Products and Orders.
 *
 * @param value the non-blank external product id
 */
public record ProductExternalId(String value) {

    public ProductExternalId {
        value = Identifiers.requireText(value, "ProductExternalId");
    }

    public static ProductExternalId of(String value) {
        return new ProductExternalId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
