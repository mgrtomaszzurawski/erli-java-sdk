package io.github.mgrtomaszzurawski.erli.domain.products;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The values of an {@link ExternalAttribute}. The wire shape depends on the attribute's
 * {@link ExternalAttributeType}, so this is a sealed hierarchy rather than a free-form blob: the compiler
 * forces a consumer to handle every shape, and an unmatched shape cannot be constructed by mistake.
 *
 * <p>The SDK targets Java 17, so the idiomatic consumer form is an {@code instanceof} chain; on Java 21
 * and later the same hierarchy also works with an exhaustive {@code switch} pattern.
 *
 * <pre>{@code
 * AttributeValues values = attribute.values();
 * if (values instanceof AttributeValues.NumericValues numeric) {
 *     render(numeric.numbers());
 * } else if (values instanceof AttributeValues.RangeValues range) {
 *     render(range.from(), range.to());
 * } else if (values instanceof AttributeValues.DictionaryValues dictionary) {
 *     render(dictionary.entries());
 * } else if (values instanceof AttributeValues.TextValues text) {
 *     render(text.texts());
 * }
 * }</pre>
 */
public sealed interface AttributeValues {

    /**
     * Numeric values ({@link ExternalAttributeType#NUMBER}).
     *
     * @param numbers the numeric values (defensively copied)
     */
    record NumericValues(List<BigDecimal> numbers) implements AttributeValues {

        public NumericValues {
            numbers = List.copyOf(numbers);
        }
    }

    /**
     * A single inclusive range ({@link ExternalAttributeType#RANGE}). Either bound may be absent, which
     * the marketplace reads as an open-ended range.
     *
     * @param from the lower bound, if present
     * @param to   the upper bound, if present
     */
    record RangeValues(Optional<BigDecimal> from, Optional<BigDecimal> to) implements AttributeValues {
    }

    /**
     * References to dictionary entries ({@link ExternalAttributeType#DICTIONARY}).
     *
     * @param entries the referenced entries (defensively copied)
     */
    record DictionaryValues(List<DictionaryValue> entries) implements AttributeValues {

        public DictionaryValues {
            entries = List.copyOf(entries);
        }
    }

    /**
     * Free-text values ({@link ExternalAttributeType#STRING}).
     *
     * @param texts the text values (defensively copied)
     */
    record TextValues(List<String> texts) implements AttributeValues {

        public TextValues {
            texts = List.copyOf(texts);
        }
    }
}
