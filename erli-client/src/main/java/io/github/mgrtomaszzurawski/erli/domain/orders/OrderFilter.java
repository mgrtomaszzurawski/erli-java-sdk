package io.github.mgrtomaszzurawski.erli.domain.orders;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A predicate narrowing {@link OrderAccess#search}.
 *
 * <p>Erli's filter grammar is recursive: a filter is either a comparison on a field, a set membership
 * test, a logical combination of other filters, or a negation of one. That shape is mirrored here as a
 * sealed hierarchy, so a consumer can build arbitrarily nested predicates and the mapper can switch
 * over them exhaustively.
 *
 * <p>Build them through the static factories rather than the records:
 * <pre>{@code
 * OrderFilter recentAndPaid = OrderFilter.and(
 *         OrderFilter.createdAfter(OffsetDateTime.now().minusDays(7)),
 *         OrderFilter.paymentCompleted());
 * }</pre>
 *
 * <p>Values are carried as strings because that is what the wire format uses for every filterable
 * field; the factories exist so callers rarely have to format one by hand.
 */
public sealed interface OrderFilter
        permits OrderFilter.Comparison, OrderFilter.Membership, OrderFilter.Combination, OrderFilter.Negation {

    /** The only {@code paymentStatus} value Erli accepts in a filter. */
    String PAYMENT_STATUS_COMPLETED = "completed";

    /** A field an order can be filtered on. */
    enum Field {
        ID,
        CREATED,
        UPDATED,
        PAYMENT_STATUS,
        USER_EMAIL
    }

    /** How a field is compared against a single value. */
    enum ComparisonOperator {
        EQUAL,
        NOT_EQUAL,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL
    }

    /** How a field is tested against a set of values. */
    enum MembershipOperator {
        IN,
        NOT_IN
    }

    /** How sub-filters are combined. */
    enum LogicalOperator {
        AND,
        OR
    }

    /**
     * A field compared against one value.
     *
     * @param field    the field being compared
     * @param operator the comparison
     * @param value    the value to compare against, already in wire form
     */
    record Comparison(Field field, ComparisonOperator operator, String value) implements OrderFilter {

        public Comparison {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(value, "value");
        }
    }

    /**
     * A field tested for membership of a set of values.
     *
     * @param field    the field being tested
     * @param operator whether the field must be in or not in the set
     * @param values   the set; never empty
     */
    record Membership(Field field, MembershipOperator operator, List<String> values) implements OrderFilter {

        public Membership {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(operator, "operator");
            values = List.copyOf(values);
            if (values.isEmpty()) {
                throw new IllegalArgumentException("A membership filter needs at least one value");
            }
        }
    }

    /**
     * Several filters combined with {@code and} or {@code or}.
     *
     * @param operator how the operands combine
     * @param operands the sub-filters; never empty
     */
    record Combination(LogicalOperator operator, List<OrderFilter> operands) implements OrderFilter {

        public Combination {
            Objects.requireNonNull(operator, "operator");
            operands = List.copyOf(operands);
            if (operands.isEmpty()) {
                throw new IllegalArgumentException("A " + operator + " filter needs at least one operand");
            }
        }
    }

    /**
     * The negation of another filter.
     *
     * @param operand the filter being negated
     */
    record Negation(OrderFilter operand) implements OrderFilter {

        public Negation {
            Objects.requireNonNull(operand, "operand");
        }
    }

    /** A field compared against a value with an explicit operator. */
    static OrderFilter compare(Field field, ComparisonOperator operator, String value) {
        return new Comparison(field, operator, value);
    }

    /** A field that must equal a value. */
    static OrderFilter equalTo(Field field, String value) {
        return new Comparison(field, ComparisonOperator.EQUAL, value);
    }

    /** A field that must be one of the given values. */
    static OrderFilter in(Field field, List<String> values) {
        return new Membership(field, MembershipOperator.IN, values);
    }

    /** A field that must be none of the given values. */
    static OrderFilter notIn(Field field, List<String> values) {
        return new Membership(field, MembershipOperator.NOT_IN, values);
    }

    /** Orders whose id is one of the given ids. */
    static OrderFilter idIn(List<OrderId> orderIds) {
        return in(Field.ID, orderIds.stream().map(OrderId::value).toList());
    }

    /** Orders created strictly after the given instant. */
    static OrderFilter createdAfter(OffsetDateTime moment) {
        return compare(Field.CREATED, ComparisonOperator.GREATER_THAN, format(moment));
    }

    /** Orders last changed strictly after the given instant — the usual "what is new for me" filter. */
    static OrderFilter updatedAfter(OffsetDateTime moment) {
        return compare(Field.UPDATED, ComparisonOperator.GREATER_THAN, format(moment));
    }

    /** Orders the buyer has paid for. */
    static OrderFilter paymentCompleted() {
        return equalTo(Field.PAYMENT_STATUS, PAYMENT_STATUS_COMPLETED);
    }

    /** Orders placed by the given buyer e-mail address. */
    static OrderFilter userEmail(String email) {
        return equalTo(Field.USER_EMAIL, email);
    }

    /** All of the given filters must hold. */
    static OrderFilter and(OrderFilter... filters) {
        return new Combination(LogicalOperator.AND, Arrays.asList(filters));
    }

    /** At least one of the given filters must hold. */
    static OrderFilter or(OrderFilter... filters) {
        return new Combination(LogicalOperator.OR, Arrays.asList(filters));
    }

    /** The given filter must not hold. */
    static OrderFilter not(OrderFilter filter) {
        return new Negation(filter);
    }

    private static String format(OffsetDateTime moment) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Objects.requireNonNull(moment, "moment"));
    }
}
