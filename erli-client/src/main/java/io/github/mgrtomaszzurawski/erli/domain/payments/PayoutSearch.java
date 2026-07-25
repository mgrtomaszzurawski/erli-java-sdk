package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.SortOrder;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSearch.ComparisonOperator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Which payouts to return, and in what order. Build with {@link #builder()}; an unfiltered search is
 * {@link #all()}.
 *
 * @param filter    the comparison to apply, if any
 * @param sortField the field to order by
 * @param order     the direction to order in
 * @param pageSize  how many payouts to fetch per request, at most {@value #MAX_PAGE_SIZE}
 */
public record PayoutSearch(
        Optional<PayoutComparison> filter,
        PayoutSortField sortField,
        SortOrder order,
        int pageSize) {

    /** Largest page the API accepts. */
    public static final int MAX_PAGE_SIZE = 200;

    /** Page size used when the caller does not choose one (the API's own default). */
    public static final int DEFAULT_PAGE_SIZE = 50;

    public PayoutSearch {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "pageSize must be between 1 and " + MAX_PAGE_SIZE + ", got " + pageSize);
        }
    }

    /** Every payout, oldest first. */
    public static PayoutSearch all() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * One comparison against a payout field. {@code createdAt} takes timestamps, {@code amount} takes
     * an amount in grosze, and {@code id} is only usable with {@code in}/{@code nin}.
     *
     * @param field    the field to compare
     * @param operator how to compare it
     * @param value    the value or values to compare against
     */
    public record PayoutComparison(PayoutFilterField field, ComparisonOperator operator, List<Object> value) {

        public PayoutComparison {
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
            // The API's payout filter has two mutually exclusive shapes: id only with in/nin, and
            // createdAt/amount only with the ordering operators. Refuse the pairings it would 400 on
            // rather than making the caller discover it from the server.
            if (field == PayoutFilterField.ID && !operator.multiValued()) {
                throw new IllegalArgumentException(
                        "payout filter on ID supports only IN or NOT_IN, got " + operator);
            }
            if (field != PayoutFilterField.ID && operator.multiValued()) {
                throw new IllegalArgumentException(
                        "payout filter on " + field + " does not support " + operator
                                + "; only the ID field can match a list");
            }
        }
    }

    /** Payout field a comparison can be applied to. */
    public enum PayoutFilterField {
        /** The payout id; the API supports it only with {@code in}/{@code nin}. */
        ID,
        /** When the payout was created. */
        CREATED_AT,
        /** The payout amount. */
        AMOUNT
    }

    /** Builder for {@link PayoutSearch}. */
    public static final class Builder {

        private PayoutComparison filter;
        private PayoutSortField sortField = PayoutSortField.CREATED_AT;
        private SortOrder order = SortOrder.ASCENDING;
        private int pageSize = DEFAULT_PAGE_SIZE;

        private Builder() {
        }

        /** Keep only payouts created at or after this instant. */
        public Builder createdFrom(OffsetDateTime from) {
            return matching(PayoutFilterField.CREATED_AT, ComparisonOperator.GREATER_THAN_OR_EQUAL, from);
        }

        /** Keep only payouts of at least this amount. */
        public Builder amountAtLeast(Money amount) {
            return matching(PayoutFilterField.AMOUNT, ComparisonOperator.GREATER_THAN_OR_EQUAL, amount);
        }

        /** Keep only the payouts with these ids. */
        public Builder matchingIds(List<Long> payoutIds) {
            this.filter = new PayoutComparison(
                    PayoutFilterField.ID, ComparisonOperator.IN, List.copyOf(payoutIds));
            return this;
        }

        /** Apply an arbitrary single-value comparison. */
        public Builder matching(PayoutFilterField field, ComparisonOperator operator, Object value) {
            this.filter = new PayoutComparison(field, operator, List.of(value));
            return this;
        }

        /** Field to order by; defaults to when the payout was created. */
        public Builder sortField(PayoutSortField value) {
            this.sortField = value;
            return this;
        }

        /** Direction to order in; defaults to ascending. */
        public Builder order(SortOrder value) {
            this.order = value;
            return this;
        }

        /** Payouts fetched per request, at most {@value #MAX_PAGE_SIZE}. */
        public Builder pageSize(int value) {
            this.pageSize = value;
            return this;
        }

        public PayoutSearch build() {
            return new PayoutSearch(Optional.ofNullable(filter), sortField, order, pageSize);
        }
    }
}
