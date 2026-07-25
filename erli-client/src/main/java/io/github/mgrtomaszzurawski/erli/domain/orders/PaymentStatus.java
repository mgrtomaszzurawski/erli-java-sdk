package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * Status of the payment attached to an order.
 *
 * @deprecated Erli deprecated the payment block embedded in an order; the authoritative source is
 *         {@code POST /payments/_search} (the Finance bucket). Kept so an order fetched today still
 *         maps losslessly. See {@link OrderPayment}.
 */
@Deprecated(since = "0.1.0")
public enum PaymentStatus {

    NEW,
    PENDING,
    WAITING_FOR_CONFIRMATION,
    COMPLETED,
    CANCELED
}
