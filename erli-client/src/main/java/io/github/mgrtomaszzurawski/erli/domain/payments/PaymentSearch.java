package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.SortOrder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Which payments to return, and in what order. Build with {@link #builder()}; an unfiltered search
 * is {@link #all()}.
 *
 * <p>The API accepts a single comparison at a time, so the builder's {@code matching*} methods
 * replace one another rather than combining.
 *
 * @param filter    the comparison to apply, if any
 * @param sortField the field to order by
 * @param order     the direction to order in
 * @param pageSize  how many payments to fetch per request, at most {@value #MAX_PAGE_SIZE}
 */
public record PaymentSearch(
        Optional<PaymentComparison> filter,
        PaymentSortField sortField,
        SortOrder order,
        int pageSize) {

    /** Largest page the API accepts. */
    public static final int MAX_PAGE_SIZE = 200;

    /** Page size used when the caller does not choose one (the API's own default). */
    public static final int DEFAULT_PAGE_SIZE = 50;

    public PaymentSearch {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "pageSize must be between 1 and " + MAX_PAGE_SIZE + ", got " + pageSize);
        }
    }

    /** Every payment, oldest first. */
    public static PaymentSearch all() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * One comparison against a payment field. The API pairs each field with a value type: {@code id}
     * takes numbers, {@code orderId} takes order ids compared lexicographically, and the two date
     * fields take timestamps.
     *
     * @param field    the field to compare
     * @param operator how to compare it
     * @param value    the value or values to compare against
     */
    public record PaymentComparison(PaymentFilterField field, ComparisonOperator operator, List<Object> value) {

        public PaymentComparison {
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(operator, "operator");
            value = List.copyOf(value);
            if (value.isEmpty()) {
                throw new IllegalArgumentException("a filter needs at least one value to compare against");
            }
            // Only in/nin take a list. Silently sending just the first of several would narrow the
            // filter without saying so, which on a money query is worse than refusing.
            if (value.size() > 1 && !operator.multiValued()) {
                throw new IllegalArgumentException(
                        "operator " + operator + " compares against a single value, got " + value.size()
                                + "; use IN or NOT_IN to match several");
            }
        }
    }

    /** Payment field a comparison can be applied to. */
    public enum PaymentFilterField {
        /** The payment id. */
        ID,
        /** The covered order's id. */
        ORDER_ID,
        /** When the payment was started. */
        CREATED_AT,
        /** When the payment finished. */
        COMPLETED_AT
    }

    /** How a comparison relates the field to the value. */
    public enum ComparisonOperator {
        /** Equal to. */
        EQUAL,
        /** Not equal to. */
        NOT_EQUAL,
        /** Greater than. */
        GREATER_THAN,
        /** Greater than or equal to. */
        GREATER_THAN_OR_EQUAL,
        /** Less than. */
        LESS_THAN,
        /** Less than or equal to. */
        LESS_THAN_OR_EQUAL,
        /** One of the given values. */
        IN,
        /** None of the given values. */
        NOT_IN;

        /** Whether this operator compares against a list rather than a single value. */
        public boolean multiValued() {
            return this == IN || this == NOT_IN;
        }
    }

    /** Builder for {@link PaymentSearch}. */
    public static final class Builder {

        private PaymentComparison filter;
        private PaymentSortField sortField = PaymentSortField.CREATED_AT;
        private SortOrder order = SortOrder.ASCENDING;
        private int pageSize = DEFAULT_PAGE_SIZE;

        private Builder() {
        }

        /** Keep only the payment with this id. */
        public Builder matchingId(long paymentId) {
            return matching(PaymentFilterField.ID, ComparisonOperator.EQUAL, paymentId);
        }

        /** Keep only payments covering this order. */
        public Builder matchingOrder(OrderId orderId) {
            return matching(PaymentFilterField.ORDER_ID, ComparisonOperator.EQUAL, orderId.value());
        }

        /** Keep only payments created at or after this instant. */
        public Builder createdFrom(OffsetDateTime from) {
            return matching(PaymentFilterField.CREATED_AT, ComparisonOperator.GREATER_THAN_OR_EQUAL, from);
        }

        /** Keep only payments completed at or before this instant. */
        public Builder completedUntil(OffsetDateTime until) {
            return matching(PaymentFilterField.COMPLETED_AT, ComparisonOperator.LESS_THAN_OR_EQUAL, until);
        }

        /** Apply an arbitrary single-value comparison. */
        public Builder matching(PaymentFilterField field, ComparisonOperator operator, Object value) {
            this.filter = new PaymentComparison(field, operator, List.of(value));
            return this;
        }

        /** Apply an {@code in}/{@code nin} comparison against several values. */
        public Builder matchingAnyOf(PaymentFilterField field, ComparisonOperator operator, List<Object> values) {
            this.filter = new PaymentComparison(field, operator, values);
            return this;
        }

        /** Field to order by; defaults to when the payment was created. */
        public Builder sortField(PaymentSortField value) {
            this.sortField = value;
            return this;
        }

        /** Direction to order in; defaults to ascending. */
        public Builder order(SortOrder value) {
            this.order = value;
            return this;
        }

        /** Payments fetched per request, at most {@value #MAX_PAGE_SIZE}. */
        public Builder pageSize(int value) {
            this.pageSize = value;
            return this;
        }

        public PaymentSearch build() {
            return new PaymentSearch(Optional.ofNullable(filter), sortField, order, pageSize);
        }
    }
}
