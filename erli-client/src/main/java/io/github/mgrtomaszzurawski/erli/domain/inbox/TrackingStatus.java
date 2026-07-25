package io.github.mgrtomaszzurawski.erli.domain.inbox;

/**
 * Where the parcel is. Mirrors the API enum.
 *
 * <p>Note the API's own caveat on {@link #SENT}: for a personal-pickup delivery it means the order is
 * ready to be collected, not that it is in transit.
 */
public enum TrackingStatus {
    PREPARING,
    WAITING_FOR_COURIER,
    SENT,
    READY_TO_PICKUP,
    ON_THE_WAY,
    READY_TO_SEND,
    TRACKING_UNAVAILABLE,
    RETURNED,
    CANCELED
}
