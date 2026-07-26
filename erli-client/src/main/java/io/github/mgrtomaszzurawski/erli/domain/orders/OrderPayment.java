package io.github.mgrtomaszzurawski.erli.domain.orders;

import io.github.mgrtomaszzurawski.erli.core.model.PaymentStatus;
import java.util.Optional;

/**
 * The payment block Erli embeds in an order.
 *
 * @param id     Erli's numeric id of the payment
 * @param status the payment's status, when Erli reports one
 * @deprecated Erli deprecated this block in favour of {@code POST /payments/_search} (the Finance
 *         bucket), which is authoritative and carries far more detail. It is still mapped so that an
 *         order fetched today round-trips losslessly, but new code should not read it.
 */
@Deprecated(since = "0.1.0", forRemoval = false)
@SuppressWarnings("deprecation") // a deprecated type may still reference itself
public record OrderPayment(long id, Optional<PaymentStatus> status) {
}
