package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Money a buyer paid, covering one or more orders.
 *
 * <p>{@link #methodCode()} and {@link #methodName()} are deliberately plain text rather than an enum:
 * the spec enumerates the operator's current method list (a few dozen bank and wallet codes), which
 * changes without a spec release. Compare against the constants you care about instead of switching
 * exhaustively.
 *
 * @param id                the payment's identifier
 * @param orderIds          the orders this payment covers; never empty
 * @param amount            the amount paid
 * @param status            where the payment is in its lifecycle
 * @param createdAt         when the payment was started
 * @param completedAt       when the payment finished
 * @param operator          the provider that settled it
 * @param methodCode        the operator's code for the method used, e.g. {@code PAYU.blik}
 * @param methodName        the operator's display name for that method, when supplied
 * @param externalPaymentId the operator's own identifier for the payment, when supplied
 */
public record Payment(
        long id,
        List<OrderId> orderIds,
        Money amount,
        PaymentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        PaymentOperator operator,
        String methodCode,
        Optional<String> methodName,
        Optional<String> externalPaymentId) implements PaymentOperation {

    public Payment {
        orderIds = List.copyOf(orderIds);
    }
}
