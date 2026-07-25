package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.util.List;
import java.util.Objects;

/**
 * A single filter for {@code POST /shipping/parcels/_search}.
 *
 * <p>Sealed over the API's two request shapes so an impossible combination cannot be constructed: a
 * scalar comparison always carries one value, {@code in}/{@code nin} always carry a list. The factory
 * methods are the only way in, and each pins the operator, so a caller cannot pair {@code >} with a
 * list or {@code in} with a single value.
 */
public sealed interface ParcelFilter permits ParcelFilter.Comparison, ParcelFilter.Membership {

    /** The attribute being filtered. */
    ParcelSearchField field();

    /** The comparison applied. */
    ParcelSearchOperator operator();

    /** A scalar comparison: one field, one operator, one value. */
    record Comparison(ParcelSearchField field, ParcelSearchOperator operator, String value)
            implements ParcelFilter {

        public Comparison {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(value, "value");
        }
    }

    /** A membership test: one field, {@code in} or {@code nin}, a list of values. */
    record Membership(ParcelSearchField field, ParcelSearchOperator operator, List<String> values)
            implements ParcelFilter {

        public Membership {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(operator, "operator");
            values = List.copyOf(Objects.requireNonNull(values, "values"));
            if (values.isEmpty()) {
                throw new IllegalArgumentException("a membership filter needs at least one value");
            }
        }
    }

    /** {@code field = value}. */
    static ParcelFilter isEqualTo(ParcelSearchField field, String value) {
        return new Comparison(field, ParcelSearchOperator.EQUAL, value);
    }

    /** {@code field != value}. */
    static ParcelFilter isNotEqualTo(ParcelSearchField field, String value) {
        return new Comparison(field, ParcelSearchOperator.NOT_EQUAL, value);
    }

    /** {@code field > value}. */
    static ParcelFilter isGreaterThan(ParcelSearchField field, String value) {
        return new Comparison(field, ParcelSearchOperator.GREATER_THAN, value);
    }

    /** {@code field >= value}. */
    static ParcelFilter isAtLeast(ParcelSearchField field, String value) {
        return new Comparison(field, ParcelSearchOperator.GREATER_THAN_OR_EQUAL, value);
    }

    /** {@code field < value}. */
    static ParcelFilter isLessThan(ParcelSearchField field, String value) {
        return new Comparison(field, ParcelSearchOperator.LESS_THAN, value);
    }

    /** {@code field <= value}. */
    static ParcelFilter isAtMost(ParcelSearchField field, String value) {
        return new Comparison(field, ParcelSearchOperator.LESS_THAN_OR_EQUAL, value);
    }

    /** {@code field in values}. */
    static ParcelFilter isAnyOf(ParcelSearchField field, List<String> values) {
        return new Membership(field, ParcelSearchOperator.IN, values);
    }

    /** {@code field nin values}. */
    static ParcelFilter isNoneOf(ParcelSearchField field, List<String> values) {
        return new Membership(field, ParcelSearchOperator.NOT_IN, values);
    }
}
