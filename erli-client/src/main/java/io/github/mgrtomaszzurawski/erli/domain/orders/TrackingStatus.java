package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * Where the parcel is, as reported by {@link DeliveryTracking}.
 */
public enum TrackingStatus {

    PREPARING,
    READY_TO_SEND,
    WAITING_FOR_COURIER,

    /**
     * Dispatched. For a personal-pickup delivery this means the order is ready to be collected, not
     * that it is in transit.
     */
    SENT,

    ON_THE_WAY,
    READY_TO_PICKUP,
    RETURNED,
    CANCELED,

    /** The carrier exposes no tracking for this parcel. */
    TRACKING_UNAVAILABLE
}
