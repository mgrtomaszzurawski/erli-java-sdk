package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A timed promotion to start on a product ({@code POST /products/{externalId}/discount}).
 *
 * <p>The marketplace freezes a product's price for the duration of a promotion. {@code unfreezeAfterwards}
 * decides what happens at {@code restoreAt}: {@code true} returns the product to normal, seller-driven
 * pricing, {@code false} leaves the price frozen at its restored value.
 *
 * @param newPrice           the promotional price
 * @param startAt            when the promotion begins
 * @param restoreAt          when the original price is restored
 * @param unfreezeAfterwards whether pricing is unfrozen once the promotion ends
 */
public record DiscountRequest(
        Money newPrice,
        OffsetDateTime startAt,
        OffsetDateTime restoreAt,
        boolean unfreezeAfterwards) {

    public DiscountRequest {
        Objects.requireNonNull(newPrice, "newPrice");
        Objects.requireNonNull(startAt, "startAt");
        Objects.requireNonNull(restoreAt, "restoreAt");
        if (!restoreAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "A discount must end after it starts: startAt=" + startAt + ", restoreAt=" + restoreAt);
        }
    }

    /** A promotion that runs between the two instants and unfreezes pricing when it ends. */
    public static DiscountRequest between(Money newPrice, OffsetDateTime startAt, OffsetDateTime restoreAt) {
        return new DiscountRequest(newPrice, startAt, restoreAt, true);
    }
}
