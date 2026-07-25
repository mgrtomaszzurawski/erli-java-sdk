package io.github.mgrtomaszzurawski.erli.domain.hooks;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import java.util.Objects;

/**
 * One line of a buyability test-fire: how many units of a product Erli should ask the shop about.
 *
 * @param productId the product's id in the shop's own system
 * @param quantity  the quantity Erli asks about (at least one)
 */
public record BuyabilityQuery(ProductExternalId productId, int quantity) {

    private static final int MIN_QUANTITY = 1;

    public BuyabilityQuery {
        Objects.requireNonNull(productId, "productId");
        if (quantity < MIN_QUANTITY) {
            throw new IllegalArgumentException("quantity must be at least " + MIN_QUANTITY);
        }
    }

    public static BuyabilityQuery of(ProductExternalId productId, int quantity) {
        return new BuyabilityQuery(productId, quantity);
    }
}
