package io.github.mgrtomaszzurawski.erli.domain.orders;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;

import java.util.Objects;
import java.util.Optional;

/**
 * What to look for, and in what order, when calling {@link OrderAccess#search}.
 *
 * <p>Page size and cursor concern one HTTP round-trip, not the result set: {@code search} returns a
 * lazy stream that walks every page, so {@link Builder#pageSize} tunes how often it goes to the
 * network, and {@link Builder#startAfter} resumes a previous walk rather than limiting it.
 *
 * <pre>{@code
 * OrderSearchRequest request = OrderSearchRequest.builder()
 *         .filter(OrderFilter.updatedAfter(lastSync))
 *         .sortBy(SortField.UPDATED)
 *         .direction(SortDirection.ASCENDING)
 *         .build();
 * }</pre>
 */
public final class OrderSearchRequest {

    /** The largest page Erli will return. */
    public static final int MAX_PAGE_SIZE = 200;

    /** The page size Erli applies when none is requested. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /** The smallest page Erli will return. */
    public static final int MIN_PAGE_SIZE = 1;

    /** Which timestamp orders are sorted on. Erli dropped sorting by id in March 2025. */
    public enum SortField {
        CREATED,
        UPDATED
    }

    /** Sort direction. */
    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }

    private final Optional<OrderFilter> filter;
    private final SortField sortField;
    private final SortDirection direction;
    private final int pageSize;
    private final Optional<Cursor> startAfter;

    private OrderSearchRequest(Builder builder) {
        this.filter = Optional.ofNullable(builder.filter);
        this.sortField = builder.sortField;
        this.direction = builder.direction;
        this.pageSize = builder.pageSize;
        this.startAfter = Optional.ofNullable(builder.startAfter);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Every order the shop has, oldest change first — the request to use for a full initial sync. */
    public static OrderSearchRequest all() {
        return builder().build();
    }

    /** The predicate narrowing the search, when one was set. */
    public Optional<OrderFilter> filter() {
        return filter;
    }

    /** Which timestamp the results are sorted on. */
    public SortField sortField() {
        return sortField;
    }

    /** Whether results run oldest-first or newest-first. */
    public SortDirection direction() {
        return direction;
    }

    /** How many orders one page holds. */
    public int pageSize() {
        return pageSize;
    }

    /** The cursor to resume after, when this request continues an earlier walk. */
    public Optional<Cursor> startAfter() {
        return startAfter;
    }

    /** Builder for {@link OrderSearchRequest}; every setting has a default. */
    public static final class Builder {

        private OrderFilter filter;
        private SortField sortField = SortField.UPDATED;
        private SortDirection direction = SortDirection.ASCENDING;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private Cursor startAfter;

        private Builder() {
        }

        /** Narrow the search. Build composite predicates with {@link OrderFilter#and}/{@link OrderFilter#or}. */
        public Builder filter(OrderFilter value) {
            this.filter = value;
            return this;
        }

        /** Sort on {@code created} or {@code updated}. Defaults to {@code updated}. */
        public Builder sortBy(SortField value) {
            this.sortField = Objects.requireNonNull(value, "sortField");
            return this;
        }

        /** Sort ascending or descending. Defaults to ascending. */
        public Builder direction(SortDirection value) {
            this.direction = Objects.requireNonNull(value, "direction");
            return this;
        }

        /**
         * How many orders to fetch per request, between {@value OrderSearchRequest#MIN_PAGE_SIZE} and
         * {@value OrderSearchRequest#MAX_PAGE_SIZE}.
         *
         * @throws IllegalArgumentException if the value is outside the range Erli accepts
         */
        public Builder pageSize(int value) {
            if (value < MIN_PAGE_SIZE || value > MAX_PAGE_SIZE) {
                throw new IllegalArgumentException(
                        "pageSize must be between " + MIN_PAGE_SIZE + " and " + MAX_PAGE_SIZE + ", got " + value);
            }
            this.pageSize = value;
            return this;
        }

        /**
         * Resume after a cursor taken from {@link Order#cursor()} of a previously seen order, instead
         * of starting from the beginning.
         */
        public Builder startAfter(Cursor value) {
            this.startAfter = value;
            return this;
        }

        public OrderSearchRequest build() {
            return new OrderSearchRequest(this);
        }
    }
}
