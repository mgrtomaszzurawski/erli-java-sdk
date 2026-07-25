package io.github.mgrtomaszzurawski.erli.domain.products;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * A product's content in one market's language. Read-only: translations are produced by the marketplace.
 *
 * @param name          the translated product name, when supplied
 * @param descriptionId the id of the translated description document, when supplied
 * @param attributes    the translated attributes (defensively copied)
 */
public record Translation(
        Optional<String> name,
        Optional<BigDecimal> descriptionId,
        List<TranslatedAttribute> attributes) {

    public Translation {
        attributes = List.copyOf(attributes);
    }
}
