package io.github.mgrtomaszzurawski.erli.domain.products;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * One node of a marketplace category path. A product carries a list of paths, each path a root-to-leaf
 * list of these nodes. Read-only: resolved by the marketplace, never sent.
 *
 * @param id   the marketplace category id, when supplied
 * @param name the category name, when supplied
 */
public record ProductCategory(Optional<BigDecimal> id, Optional<String> name) {
}
