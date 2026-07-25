package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.time.OffsetDateTime;

/**
 * A timed promotion currently recorded against a product
 * ({@code GET|POST /products/{externalId}/discount}).
 *
 * @param externalId         the product the promotion belongs to
 * @param shopId             the shop that owns the product
 * @param newPrice           the promotional price
 * @param startAt            when the promotion begins
 * @param restoreAt          when the original price is restored
 * @param unfreezeAfterwards whether pricing is unfrozen once the promotion ends
 */
public record Discount(
        ProductExternalId externalId,
        long shopId,
        io.github.mgrtomaszzurawski.erli.core.model.Money newPrice,
        OffsetDateTime startAt,
        OffsetDateTime restoreAt,
        boolean unfreezeAfterwards) {

    /** Whether the promotion is running at the given instant. */
    public boolean isActiveAt(OffsetDateTime instant) {
        return !instant.isBefore(startAt) && instant.isBefore(restoreAt);
    }
}
