package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.AttributeId;
import java.util.List;
import java.util.Objects;

/**
 * The allowed values of one {@link AttributeType#DICTIONARY} attribute, as returned for a category.
 *
 * <p>{@link #values()} and {@link #valueIds()} are positional: the value at index {@code i} is
 * identified by the id at index {@code i}. The API has been observed to return both lists at equal
 * length; if a future response disagrees, the shorter list bounds what {@link #valueAt(int)} exposes.
 *
 * @param attributeId the attribute these values belong to
 * @param values the human-readable Polish value labels
 * @param valueIds the identifiers of those values, positionally aligned with {@link #values()}
 */
public record AttributeValues(AttributeId attributeId, List<String> values, List<String> valueIds) {

    public AttributeValues {
        Objects.requireNonNull(attributeId, "attributeId");
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        valueIds = List.copyOf(Objects.requireNonNull(valueIds, "valueIds"));
    }

    /** How many values are addressable as an id/label pair. */
    public int pairCount() {
        return Math.min(values.size(), valueIds.size());
    }

    /**
     * The id/label pair at {@code index}.
     *
     * @param index a position below {@link #pairCount()}
     * @return the pair, as {@code [valueId, value]}
     * @throws IndexOutOfBoundsException if {@code index} is not a valid pair position
     */
    public List<String> valueAt(int index) {
        if (index < 0 || index >= pairCount()) {
            throw new IndexOutOfBoundsException(
                    "No attribute value pair at index " + index + " (pairCount=" + pairCount() + ")");
        }
        return List.of(valueIds.get(index), values.get(index));
    }
}
