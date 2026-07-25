package io.github.mgrtomaszzurawski.erli.domain.products;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * One component of a seller-declared product set, addressed by the seller's own meta-product id.
 *
 * @param externalMetaProductId the seller-assigned meta-product id, when supplied
 * @param quantity              how many units of it the set contains, when supplied
 */
public record ExternalProductSetItem(
        Optional<String> externalMetaProductId,
        Optional<BigDecimal> quantity) {
}
