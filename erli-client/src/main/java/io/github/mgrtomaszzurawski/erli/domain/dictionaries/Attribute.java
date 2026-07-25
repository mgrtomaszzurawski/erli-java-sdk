package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.AttributeId;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * A product attribute defined for a category. For a {@link AttributeType#DICTIONARY} attribute the
 * allowed values come from {@link DictionariesAccess#attributeValues}.
 *
 * @param id the attribute identifier
 * @param name the Polish attribute label
 * @param required whether a product in the category must supply this attribute
 * @param variantable whether the attribute may distinguish variants of one product
 * @param sameValueForcedOnVariants whether all variants must share one value for this attribute
 * @param type how the attribute's values are shaped
 * @param maxValues how many values may be supplied at once
 * @param min the lower bound for numeric attributes, when stated
 * @param max the upper bound for numeric attributes, when stated
 * @param precision the number of decimal places for numeric attributes, when stated
 * @param unit the unit of measure, when stated
 */
public record Attribute(
        AttributeId id,
        String name,
        boolean required,
        boolean variantable,
        boolean sameValueForcedOnVariants,
        AttributeType type,
        BigDecimal maxValues,
        Optional<BigDecimal> min,
        Optional<BigDecimal> max,
        Optional<BigDecimal> precision,
        Optional<String> unit) {

    public Attribute {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(maxValues, "maxValues");
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        Objects.requireNonNull(precision, "precision");
        Objects.requireNonNull(unit, "unit");
    }
}
