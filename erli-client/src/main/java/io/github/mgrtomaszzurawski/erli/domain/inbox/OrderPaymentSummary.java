package io.github.mgrtomaszzurawski.erli.domain.inbox;

import io.github.mgrtomaszzurawski.erli.core.model.PaymentStatus;
import java.util.Objects;
import java.util.Optional;

/**
 * The payment attached to the order, as summarised inside an order event.
 *
 * <p>The API marks this object as superseded by the payments endpoints, so treat it as a hint and read
 * {@code client.payments()} for anything authoritative. It is mapped because it is part of the event
 * payload the shop receives.
 *
 * @param id     the marketplace's payment id
 * @param status the payment's status, when the API reports one this SDK version recognises — an
 *               unrecognised status decodes to absent (CORE-12)
 * @deprecated the API superseded this in favour of the payments endpoints; read {@code client.payments()}
 *         instead. Kept so an order event still maps losslessly.
 */
@Deprecated(since = "0.1.0", forRemoval = false)
public record OrderPaymentSummary(long id, Optional<PaymentStatus> status) {

    public OrderPaymentSummary {
        Objects.requireNonNull(status, "status");
    }
}
