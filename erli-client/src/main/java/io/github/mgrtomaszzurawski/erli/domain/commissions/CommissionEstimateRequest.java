package io.github.mgrtomaszzurawski.erli.domain.commissions;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.util.Objects;

/**
 * What to estimate the marketplace commission for: a quantity of one item, priced per unit, listed
 * in one category. Build with {@link #builder()}.
 *
 * <p>The category must be a <strong>leaf</strong> category — the API rejects a non-leaf id with a
 * validation error ({@code "invalid query for non-leaf category"}). Find leaves through
 * {@code client.dictionaries()}.
 *
 * @param categoryId the leaf category the item would be listed in
 * @param unitPrice  the price of a single unit
 * @param quantity   how many units; the API defaults this to {@value #DEFAULT_QUANTITY}
 */
public record CommissionEstimateRequest(CategoryId categoryId, Money unitPrice, int quantity) {

    /** The quantity assumed when the caller does not set one (matches the API default). */
    public static final int DEFAULT_QUANTITY = 1;

    public CommissionEstimateRequest {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1, got " + quantity);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link CommissionEstimateRequest}; category and unit price are required. */
    public static final class Builder {

        private CategoryId categoryId;
        private Money unitPrice;
        private int quantity = DEFAULT_QUANTITY;

        private Builder() {
        }

        /** The leaf category the item would be listed in. */
        public Builder categoryId(CategoryId value) {
            this.categoryId = value;
            return this;
        }

        /** The price of a single unit. */
        public Builder unitPrice(Money value) {
            this.unitPrice = value;
            return this;
        }

        /** How many units to price; defaults to {@value #DEFAULT_QUANTITY}. */
        public Builder quantity(int value) {
            this.quantity = value;
            return this;
        }

        public CommissionEstimateRequest build() {
            return new CommissionEstimateRequest(categoryId, unitPrice, quantity);
        }
    }
}
