package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.PaymentStatus;
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
 * <p>Both are {@link Optional} for the same reason. Per the SDK's unknown-enum policy (CORE-12) a
 * growing enum must not fail the read: when the operator uses a method added after this SDK's spec
 * snapshot, the value decodes as absent rather than throwing. Everything else about the payment is
 * still there — only the method label is unknown.
 *
 * <p>Empty <em>is</em> the sentinel here. Since these are already plain text, an
 * {@code "UNRECOGNIZED"} marker string would be indistinguishable from a real method code, whereas
 * an empty {@code Optional} cannot be mistaken for one. The trade-off is that "the server sent no
 * method" and "the server sent a method we do not know" both read as empty — the raw value is gone
 * before the mapper runs.
 *
 * @param id                the payment's identifier
 * @param orderIds          the orders this payment covers; never empty
 * @param amount            the amount paid
 * @param status            where the payment is in its lifecycle
 * @param createdAt         when the payment was started
 * @param completedAt       when the payment finished; empty while it is still in flight
 * @param operator          the provider that settled it
 * @param methodCode        the operator's code for the method used, e.g. {@code PAYU.blik};
 *                          empty when the operator used a method this SDK does not recognise
 * @param methodName        the operator's display name for that method, when supplied
 * @param externalPaymentId the operator's own identifier for the payment, when supplied
 */
public record Payment(
        long id,
        List<OrderId> orderIds,
        Money amount,
        PaymentStatus status,
        OffsetDateTime createdAt,
        Optional<OffsetDateTime> completedAt,
        PaymentOperator operator,
        Optional<String> methodCode,
        Optional<String> methodName,
        Optional<String> externalPaymentId) implements PaymentOperation {

    public Payment {
        orderIds = List.copyOf(orderIds);
    }
}
