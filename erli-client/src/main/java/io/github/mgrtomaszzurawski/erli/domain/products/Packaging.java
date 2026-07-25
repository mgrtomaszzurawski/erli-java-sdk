package io.github.mgrtomaszzurawski.erli.domain.products;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * A product's packaging characteristics, used by the marketplace to price and route delivery.
 *
 * @param tags   packaging tags declared for the product (defensively copied)
 * @param weight the packed weight in kilograms, when supplied
 */
public record Packaging(List<String> tags, Optional<BigDecimal> weight) {

    public Packaging {
        tags = List.copyOf(tags);
    }
}
