package io.github.mgrtomaszzurawski.erli.domain.products;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * One component of a marketplace product set: which meta-product, and how many of it.
 *
 * @param metaProductId the marketplace meta-product id, when supplied
 * @param quantity      how many units of it the set contains, when supplied
 */
public record ProductSetItem(Optional<Integer> metaProductId, Optional<BigDecimal> quantity) {
}
