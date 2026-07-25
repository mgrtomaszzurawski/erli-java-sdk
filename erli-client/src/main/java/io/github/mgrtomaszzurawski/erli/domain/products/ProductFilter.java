package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;

/**
 * A predicate over the product catalog, used by {@code POST /products/_search}.
 *
 * <p>Filters compose: leaves compare a field to a value or test membership in a set, and
 * {@link #and}/{@link #or}/{@link #not} combine them to any depth. The hierarchy is sealed, so the wire
 * form and the domain form cannot drift apart, and a filter that cannot be expressed cannot be built.
 *
 * <p>Field/operator pairings the marketplace rejects are rejected here instead, at construction, with a
 * message naming the field — an ordered comparison on {@link ProductFilterField#STATUS} is a mistake
 * worth catching before the round trip.
 *
 * <pre>{@code
 * ProductFilter active = ProductFilter.and(
 *         ProductFilter.equalTo(ProductFilterField.STATUS, "active"),
 *         ProductFilter.not(ProductFilter.equalTo(ProductFilterField.ARCHIVED, "true")),
 *         ProductFilter.greaterThan(ProductFilterField.STOCK, "0"));
 * }</pre>
 *
 * <p>Values are carried as text because Erli's filter values are polymorphic (string, number, date or
 * boolean depending on the field); the marketplace parses them against the field's own type.
 */
public sealed interface ProductFilter {

    /** Compare a field to a value. */
    static ProductFilter compare(ProductFilterField field, ComparisonOperator operator, String value) {
        return new Comparison(field, operator, value);
    }

    /** {@code field = value}. */
    static ProductFilter equalTo(ProductFilterField field, String value) {
        return compare(field, ComparisonOperator.EQUALS, value);
    }

    /** {@code field != value}. */
    static ProductFilter notEqualTo(ProductFilterField field, String value) {
        return compare(field, ComparisonOperator.NOT_EQUALS, value);
    }

    /** {@code field > value}. */
    static ProductFilter greaterThan(ProductFilterField field, String value) {
        return compare(field, ComparisonOperator.GREATER_THAN, value);
    }

    /** {@code field >= value}. */
    static ProductFilter atLeast(ProductFilterField field, String value) {
        return compare(field, ComparisonOperator.GREATER_OR_EQUAL, value);
    }

    /** {@code field < value}. */
    static ProductFilter lessThan(ProductFilterField field, String value) {
        return compare(field, ComparisonOperator.LESS_THAN, value);
    }

    /** {@code field <= value}. */
    static ProductFilter atMost(ProductFilterField field, String value) {
        return compare(field, ComparisonOperator.LESS_OR_EQUAL, value);
    }

    /** {@code field} is one of {@code values}. */
    static ProductFilter in(ProductFilterField field, List<String> values) {
        return new Membership(field, true, values);
    }

    /** {@code field} is none of {@code values}. */
    static ProductFilter notIn(ProductFilterField field, List<String> values) {
        return new Membership(field, false, values);
    }

    /** Every operand must match. */
    static ProductFilter and(ProductFilter... operands) {
        return new Junction(true, List.of(operands));
    }

    /** At least one operand must match. */
    static ProductFilter or(ProductFilter... operands) {
        return new Junction(false, List.of(operands));
    }

    /** The operand must not match. */
    static ProductFilter not(ProductFilter operand) {
        return new Negation(operand);
    }

    /**
     * A field compared to a single value.
     *
     * @param field    the field to test
     * @param operator how to compare
     * @param value    the value to compare against, as text
     */
    record Comparison(ProductFilterField field, ComparisonOperator operator, String value)
            implements ProductFilter {

        public Comparison {
            if (field == null || operator == null) {
                throw new IllegalArgumentException("A comparison needs both a field and an operator");
            }
            if (value == null) {
                throw new IllegalArgumentException(
                        "A comparison on '" + field + "' needs a value; use a membership filter for sets");
            }
            if (operator.requiresOrderedField() && !field.supportsOrderedComparison()) {
                throw new IllegalArgumentException(
                        "Erli does not support " + operator + " on '" + field
                                + "'; that field accepts equality only");
            }
        }
    }

    /**
     * A field tested against a set of values.
     *
     * @param field    the field to test
     * @param included {@code true} for {@code in}, {@code false} for {@code nin}
     * @param values   the set to test against (defensively copied, must not be empty)
     */
    record Membership(ProductFilterField field, boolean included, List<String> values)
            implements ProductFilter {

        public Membership {
            if (field == null) {
                throw new IllegalArgumentException("A membership filter needs a field");
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException(
                        "A membership filter on '" + field + "' needs at least one value");
            }
            values = List.copyOf(values);
        }
    }

    /**
     * Several filters combined.
     *
     * @param conjunction {@code true} for {@code and}, {@code false} for {@code or}
     * @param operands    the combined filters (defensively copied, must not be empty)
     */
    record Junction(boolean conjunction, List<ProductFilter> operands) implements ProductFilter {

        public Junction {
            if (operands == null || operands.isEmpty()) {
                throw new IllegalArgumentException("A junction needs at least one operand");
            }
            operands = List.copyOf(operands);
        }
    }

    /**
     * The negation of a filter.
     *
     * @param operand the filter to negate
     */
    record Negation(ProductFilter operand) implements ProductFilter {

        public Negation {
            if (operand == null) {
                throw new IllegalArgumentException("A negation needs an operand");
            }
        }
    }
}
