package io.github.mgrtomaszzurawski.erli.domain.billing;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Which settlement entries to return. Every criterion is optional; an empty filter returns the whole
 * history, newest first. Build with {@link #builder()}.
 *
 * @param type           entry type, from {@code GET /dictionaries/billingEntryTypes}
 * @param fromOccurredAt include only entries at or after this instant
 * @param toOccurredAt   include only entries at or before this instant
 * @param orderId        include only entries for this order
 * @param shopId         include only entries for this shop (the company history spans all shops)
 * @param productId      include only entries for this product
 * @param pageSize       how many entries to fetch per request; the API caps this at {@value #MAX_PAGE_SIZE}
 */
public record BillingEntryFilter(
        Optional<String> type,
        Optional<OffsetDateTime> fromOccurredAt,
        Optional<OffsetDateTime> toOccurredAt,
        Optional<OrderId> orderId,
        Optional<Long> shopId,
        Optional<Long> productId,
        int pageSize) {

    /** Largest page the API accepts. */
    public static final int MAX_PAGE_SIZE = 500;

    /** Page size used when the caller does not choose one (the API's own default). */
    public static final int DEFAULT_PAGE_SIZE = 100;

    public BillingEntryFilter {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "pageSize must be between 1 and " + MAX_PAGE_SIZE + ", got " + pageSize);
        }
    }

    /** An empty filter: the whole history, newest first. */
    public static BillingEntryFilter all() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link BillingEntryFilter}; every criterion is optional. */
    public static final class Builder {

        private String type;
        private OffsetDateTime fromOccurredAt;
        private OffsetDateTime toOccurredAt;
        private OrderId orderId;
        private Long shopId;
        private Long productId;
        private int pageSize = DEFAULT_PAGE_SIZE;

        private Builder() {
        }

        /** Entry type, as listed by {@code GET /dictionaries/billingEntryTypes}. */
        public Builder type(String value) {
            this.type = value;
            return this;
        }

        /** Include only entries at or after this instant. */
        public Builder fromOccurredAt(OffsetDateTime value) {
            this.fromOccurredAt = value;
            return this;
        }

        /** Include only entries at or before this instant. */
        public Builder toOccurredAt(OffsetDateTime value) {
            this.toOccurredAt = value;
            return this;
        }

        /** Include only entries raised against this order. */
        public Builder orderId(OrderId value) {
            this.orderId = value;
            return this;
        }

        /** Include only entries for this shop. */
        public Builder shopId(long value) {
            this.shopId = value;
            return this;
        }

        /** Include only entries for this product. */
        public Builder productId(long value) {
            this.productId = value;
            return this;
        }

        /** Entries fetched per request, at most {@value #MAX_PAGE_SIZE}. */
        public Builder pageSize(int value) {
            this.pageSize = value;
            return this;
        }

        public BillingEntryFilter build() {
            return new BillingEntryFilter(
                    Optional.ofNullable(type),
                    Optional.ofNullable(fromOccurredAt),
                    Optional.ofNullable(toOccurredAt),
                    Optional.ofNullable(orderId),
                    Optional.ofNullable(shopId),
                    Optional.ofNullable(productId),
                    pageSize);
        }
    }
}
