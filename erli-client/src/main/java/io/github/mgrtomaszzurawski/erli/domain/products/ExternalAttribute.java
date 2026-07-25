package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Optional;

/**
 * A seller- or integration-supplied product attribute, before the marketplace matches it against its own
 * catalog (the matched result appears as a {@link ProductAttribute}).
 *
 * <p>The {@code type} selects which {@link AttributeValues} variant {@code values} carries.
 *
 * @param id     the attribute identifier as text (polymorphic on the wire: string or number)
 * @param name   the attribute name, when supplied
 * @param source where the attribute came from
 * @param type   the value shape
 * @param index  the display position among the product's attributes, when supplied
 * @param values the values, in the shape selected by {@code type}
 * @param unit   the unit of measure, when the attribute has one
 */
public record ExternalAttribute(
        Optional<String> id,
        Optional<String> name,
        Optional<ExternalSource> source,
        Optional<ExternalAttributeType> type,
        Optional<Integer> index,
        AttributeValues values,
        Optional<String> unit) {
}
