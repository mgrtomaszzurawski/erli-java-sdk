package io.github.mgrtomaszzurawski.erli.domain.products;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * A product attribute as resolved by the marketplace catalog — the matched counterpart of an
 * {@link ExternalAttribute}. Read-only: it appears on {@code GET /products/{externalId}} and is never sent.
 *
 * @param id       the marketplace attribute id, when supplied
 * @param name     the attribute name, when supplied
 * @param values   the resolved values in their wire shape
 * @param valueIds the ids of the matched dictionary values (defensively copied)
 * @param unit     the unit of measure, when the attribute has one
 */
public record ProductAttribute(
        Optional<BigDecimal> id,
        Optional<String> name,
        AttributeValues values,
        List<BigDecimal> valueIds,
        Optional<String> unit) {

    public ProductAttribute {
        valueIds = List.copyOf(valueIds);
    }
}
