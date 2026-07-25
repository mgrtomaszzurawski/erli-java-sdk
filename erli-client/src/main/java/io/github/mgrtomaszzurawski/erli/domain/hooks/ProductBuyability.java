package io.github.mgrtomaszzurawski.erli.domain.hooks;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.util.Objects;
import java.util.Optional;

/**
 * What the shop's own {@code checkBuyability} hook answered for one product.
 *
 * <p>Both answers are optional by contract: the API declares {@code status} explicitly nullable, and
 * a shop may report availability without a stock figure.
 *
 * @param productId the product's id in the shop's own system
 * @param status    whether the shop reports the product as sellable, if it said — the API declares
 *                  this property explicitly nullable, so absent is a stated answer here, not only the
 *                  fallback an unrecognised value decodes to
 * @param stock     the units the shop reports available, if it said
 */
public record ProductBuyability(
        ProductExternalId productId,
        Optional<BuyabilityStatus> status,
        Optional<Integer> stock) {

    public ProductBuyability {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(stock, "stock");
    }
}
