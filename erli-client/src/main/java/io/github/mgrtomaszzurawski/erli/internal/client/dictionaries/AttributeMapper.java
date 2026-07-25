package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.AttributeId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attribute;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttributeType;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttributeValues;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeValuesResponseInner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated Layer-1 attribute types to the public {@link Attribute} and
 * {@link AttributeValues} domain records. Internal: never exported.
 */
final class AttributeMapper {

    private AttributeMapper() {
    }

    static Attribute toAttribute(AttributeResponseInner rawAttribute) {
        Objects.requireNonNull(rawAttribute, "raw AttributeResponseInner");
        return new Attribute(
                AttributeId.of(String.valueOf(requireId(rawAttribute))),
                requireName(rawAttribute),
                // The spec marks these three as required, but the live API omits them on some
                // attributes; a missing flag means "not set", so absent maps to false rather than throwing.
                Boolean.TRUE.equals(rawAttribute.getRequired()),
                Boolean.TRUE.equals(rawAttribute.getVariantable()),
                Boolean.TRUE.equals(rawAttribute.getSameValueForcedOnVariants()),
                toAttributeType(rawAttribute),
                requireMaxValues(rawAttribute),
                Optional.ofNullable(rawAttribute.getMin()),
                Optional.ofNullable(rawAttribute.getMax()),
                Optional.ofNullable(rawAttribute.getPrecision()),
                Optional.ofNullable(rawAttribute.getUnit()));
    }

    static AttributeValues toAttributeValues(AttributeValuesResponseInner rawValues) {
        Objects.requireNonNull(rawValues, "raw AttributeValuesResponseInner");
        Integer id = rawValues.getId();
        if (id == null) {
            throw new IllegalStateException("AttributeValues is missing the required 'id' field");
        }
        return new AttributeValues(
                AttributeId.of(String.valueOf(id)),
                rawValues.getValues() == null ? List.of() : List.copyOf(rawValues.getValues()),
                rawValues.getValueIds() == null ? List.of() : List.copyOf(rawValues.getValueIds()));
    }

    private static int requireId(AttributeResponseInner rawAttribute) {
        Integer id = rawAttribute.getId();
        if (id == null) {
            throw new IllegalStateException("Attribute is missing the required 'id' field");
        }
        return id;
    }

    private static String requireName(AttributeResponseInner rawAttribute) {
        String name = rawAttribute.getName();
        if (name == null) {
            throw new IllegalStateException("Attribute is missing the required 'name' field");
        }
        return name;
    }

    private static BigDecimal requireMaxValues(AttributeResponseInner rawAttribute) {
        BigDecimal maxValues = rawAttribute.getMaxValues();
        if (maxValues == null) {
            throw new IllegalStateException("Attribute is missing the required 'maxValues' field");
        }
        return maxValues;
    }

    private static AttributeType toAttributeType(AttributeResponseInner rawAttribute) {
        AttributeResponseInner.TypeEnum type = rawAttribute.getType();
        if (type == null) {
            throw new IllegalStateException("Attribute is missing the required 'type' field");
        }
        // Explicit switch rather than valueOf(name()): this taxonomy is closed and structural, so a new
        // upstream constant must become a compile error here, not a silent runtime surprise.
        return switch (type) {
            case NUMBER -> AttributeType.NUMBER;
            case RANGE -> AttributeType.RANGE;
            case DICTIONARY -> AttributeType.DICTIONARY;
            case STRING -> AttributeType.STRING;
        };
    }
}
