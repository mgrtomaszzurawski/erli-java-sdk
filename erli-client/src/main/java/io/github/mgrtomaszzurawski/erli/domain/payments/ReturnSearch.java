package io.github.mgrtomaszzurawski.erli.domain.payments;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Which operator transactions to return. Build with {@link #builder()}.
 *
 * <p>Unlike the payment and payout searches, this one is <strong>page-numbered rather than
 * cursor-based</strong>, and the event date range is required. Dates are whole days: the API rejects
 * timestamps here even though the spec types these fields as date-times (see
 * {@code KNOWN-SERVER-BEHAVIORS.md}).
 *
 * @param eventDateFrom    first day of the event range; required
 * @param eventDateTo      last day of the event range; required
 * @param creationDateFrom first day of the creation range, if narrowing by it
 * @param creationDateTo   last day of the creation range, if narrowing by it
 * @param types            operator transaction types to include; empty means all
 * @param market           the marketplace to search
 * @param pageSize         how many transactions to fetch per request, at most {@value #MAX_PAGE_SIZE}
 */
public record ReturnSearch(
        LocalDate eventDateFrom,
        LocalDate eventDateTo,
        Optional<LocalDate> creationDateFrom,
        Optional<LocalDate> creationDateTo,
        List<String> types,
        Market market,
        int pageSize) {

    /** Largest page the API accepts. */
    public static final int MAX_PAGE_SIZE = 1000;

    /** Page size used when the caller does not choose one (the API's own default). */
    public static final int DEFAULT_PAGE_SIZE = 100;

    public ReturnSearch {
        Objects.requireNonNull(eventDateFrom, "eventDateFrom");
        Objects.requireNonNull(eventDateTo, "eventDateTo");
        if (eventDateTo.isBefore(eventDateFrom)) {
            throw new IllegalArgumentException(
                    "eventDateTo " + eventDateTo + " must not be before eventDateFrom " + eventDateFrom);
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "pageSize must be between 1 and " + MAX_PAGE_SIZE + ", got " + pageSize);
        }
        types = List.copyOf(types);
    }

    /**
     * Search the required event-date range with every other criterion left at its default.
     *
     * @param eventDateFrom first day of the event range
     * @param eventDateTo   last day of the event range
     * @return a builder-free search over that range
     */
    public static ReturnSearch between(LocalDate eventDateFrom, LocalDate eventDateTo) {
        return builder(eventDateFrom, eventDateTo).build();
    }

    /**
     * Start building a search over the required event-date range.
     *
     * @param eventDateFrom first day of the event range
     * @param eventDateTo   last day of the event range
     * @return a builder seeded with the range
     */
    public static Builder builder(LocalDate eventDateFrom, LocalDate eventDateTo) {
        return new Builder(eventDateFrom, eventDateTo);
    }

    /** Builder for {@link ReturnSearch}; the event-date range is required up front. */
    public static final class Builder {

        private final LocalDate eventDateFrom;
        private final LocalDate eventDateTo;
        private LocalDate creationDateFrom;
        private LocalDate creationDateTo;
        private List<String> types = List.of();
        private Market market = Market.PL;
        private int pageSize = DEFAULT_PAGE_SIZE;

        private Builder(LocalDate eventDateFrom, LocalDate eventDateTo) {
            this.eventDateFrom = eventDateFrom;
            this.eventDateTo = eventDateTo;
        }

        /** Narrow to transactions created on or after this day. */
        public Builder creationDateFrom(LocalDate value) {
            this.creationDateFrom = value;
            return this;
        }

        /** Narrow to transactions created on or before this day. */
        public Builder creationDateTo(LocalDate value) {
            this.creationDateTo = value;
            return this;
        }

        /**
         * Restrict to these operator transaction types, e.g. {@code PAYOUT} or {@code CHARGEBACK}.
         * Free text rather than an enum: the operator's type list is not versioned with the spec.
         */
        public Builder types(List<String> value) {
            this.types = List.copyOf(value);
            return this;
        }

        /** Marketplace to search; defaults to Poland. */
        public Builder market(Market value) {
            this.market = value;
            return this;
        }

        /** Transactions fetched per request, at most {@value #MAX_PAGE_SIZE}. */
        public Builder pageSize(int value) {
            this.pageSize = value;
            return this;
        }

        public ReturnSearch build() {
            return new ReturnSearch(
                    eventDateFrom,
                    eventDateTo,
                    Optional.ofNullable(creationDateFrom),
                    Optional.ofNullable(creationDateTo),
                    types,
                    market,
                    pageSize);
        }
    }
}
