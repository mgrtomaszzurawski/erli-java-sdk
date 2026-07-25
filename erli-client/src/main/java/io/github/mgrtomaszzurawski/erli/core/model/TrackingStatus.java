package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Where the parcel is, as reported by an order's delivery tracking.
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
