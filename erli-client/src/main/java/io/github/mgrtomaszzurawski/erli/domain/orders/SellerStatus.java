package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * The order's status in the <em>seller's</em> own system. This is the writable status: it is what
 * {@code PATCH /orders/{id}/status} sets, via {@link OrderAccess#changeStatus}. The marketplace-side
 * {@link OrderStatus} is read-only.
 */
public enum SellerStatus {

    CREATED,
    CANCELED,
    READY_TO_PROCESS,
    IN_PROGRESS,
    SENT,
    READY_TO_PICKUP,
    RECEIVED,
    RETURNED,
    RETURNING_TO_SENDER,

    /**
     * The seller system reported no usable status. Erli accepts this value on write, so it is kept on
     * the enum rather than being modelled as an absent value.
     */
    UNKNOWN
}
