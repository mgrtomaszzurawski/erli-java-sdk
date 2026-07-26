package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import java.time.OffsetDateTime;

/**
 * Money transferred out to the seller.
 *
 * @param id        the payout's identifier
 * @param amount    the amount transferred
 * @param createdAt when the payout was created
 * @param operator  the provider that made the transfer
 */
public record Payout(
        long id,
        Money amount,
        OffsetDateTime createdAt,
        PaymentOperator operator) implements PaymentOperation {
}
