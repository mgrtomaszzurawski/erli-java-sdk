package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.domain.products.AttributeValues;
import io.github.mgrtomaszzurawski.erli.domain.products.DictionaryValue;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf1Values;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf2ValuesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOfId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps the polymorphic attribute {@code values} payload to the sealed {@link AttributeValues} hierarchy.
 *
 * <p>Two Layer-1 shapes reach this mapper. The typed {@code anyOf} branches of {@code externalAttributes}
 * arrive as generated classes; the response's own {@code attributes[].values} and the translation
 * attribute values arrive as {@code Object}, because the spec's array-branch composites are collapsed to
 * free-form objects during generation (see KNOWN-SERVER-BEHAVIORS). The untyped path therefore inspects
 * the decoded JSON structure and picks the matching variant, defaulting to text values — the shape that
 * loses the least information — when nothing else fits. Internal: never exported.
 */
final class AttributeValueMapper {

    private static final String RANGE_FROM_KEY = "from";
    private static final String RANGE_TO_KEY = "to";
    private static final String DICTIONARY_ID_KEY = "id";
    private static final String DICTIONARY_NAME_KEY = "name";

    private AttributeValueMapper() {
    }

    /** Numeric values, from the typed {@code anyOf} branch. */
    static AttributeValues fromNumbers(List<BigDecimal> numbers) {
        return new AttributeValues.NumericValues(ProductValues.orEmpty(numbers));
    }

    /** A from/to range, from the typed {@code anyOf} branch. */
    static AttributeValues fromRange(ProductCreateExternalAttributesInnerAnyOf1Values range) {
        if (range == null) {
            return new AttributeValues.RangeValues(Optional.empty(), Optional.empty());
        }
        return new AttributeValues.RangeValues(
                Optional.ofNullable(range.getFrom()),
                Optional.ofNullable(range.getTo()));
    }

    /** Dictionary references, from the typed {@code anyOf} branch. */
    static AttributeValues fromDictionary(List<ProductCreateExternalAttributesInnerAnyOf2ValuesInner> entries) {
        return new AttributeValues.DictionaryValues(
                ProductValues.mapEach(entries, entry -> new DictionaryValue(
                        identifierText(entry.getId()),
                        Optional.ofNullable(entry.getName()))));
    }

    /** Free-text values, from the typed {@code anyOf} branch. */
    static AttributeValues fromTexts(List<String> texts) {
        return new AttributeValues.TextValues(ProductValues.orEmpty(texts));
    }

    /** The polymorphic identifier of a dictionary entry (string or number on the wire) as text. */
    static String identifierText(ProductCreateExternalAttributesInnerAnyOfId identifier) {
        return identifier == null ? null : String.valueOf(identifier.getActualInstance());
    }

    /**
     * Values decoded as a free-form object: a list of numbers, a list of strings, a list of
     * {@code {id, name}} objects, or a single {@code {from, to}} range.
     */
    static AttributeValues fromUntyped(Object values) {
        if (values instanceof Map<?, ?> range) {
            return untypedRange(range);
        }
        if (values instanceof List<?> items) {
            return untypedList(items);
        }
        return new AttributeValues.TextValues(
                values == null ? List.of() : List.of(String.valueOf(values)));
    }

    private static AttributeValues untypedRange(Map<?, ?> range) {
        return new AttributeValues.RangeValues(
                toNumber(range.get(RANGE_FROM_KEY)),
                toNumber(range.get(RANGE_TO_KEY)));
    }

    private static AttributeValues untypedList(List<?> items) {
        if (items.isEmpty()) {
            return new AttributeValues.TextValues(List.of());
        }
        if (items.get(0) instanceof Map) {
            List<DictionaryValue> entries = new ArrayList<>(items.size());
            for (Object item : items) {
                Map<?, ?> entry = (Map<?, ?>) item;
                Object identifier = entry.get(DICTIONARY_ID_KEY);
                Object name = entry.get(DICTIONARY_NAME_KEY);
                entries.add(new DictionaryValue(
                        identifier == null ? null : String.valueOf(identifier),
                        Optional.ofNullable(name).map(String::valueOf)));
            }
            return new AttributeValues.DictionaryValues(entries);
        }
        if (items.get(0) instanceof Number) {
            List<BigDecimal> numbers = new ArrayList<>(items.size());
            for (Object item : items) {
                numbers.add(new BigDecimal(String.valueOf(item)));
            }
            return new AttributeValues.NumericValues(numbers);
        }
        return new AttributeValues.TextValues(items.stream().map(String::valueOf).toList());
    }

    private static Optional<BigDecimal> toNumber(Object value) {
        return value == null ? Optional.empty() : Optional.of(new BigDecimal(String.valueOf(value)));
    }
}
