package io.github.mgrtomaszzurawski.erli.domain.shipping;

/**
 * Lifecycle status of a parcel, from creation through delivery, return or failure.
 *
 * <p>The same status vocabulary is shared by Erli-handled parcels ({@code /shipping/parcels}) and
 * externally shipped ones ({@code /shipping/external}); the external endpoints accept only the subset
 * a seller can set by hand. Values mirror the API vocabulary but are mapped explicitly, so a new
 * upstream status becomes a compile error in the mapper rather than a runtime surprise.
 */
public enum ParcelStatus {

    /** Created in the SDK/panel, not yet handed to the carrier. */
    PREPARING,
    /** Ready to be handed over. */
    READY_TO_SEND,
    /** Waiting for the courier pickup. */
    WAITING_FOR_COURIER,
    /** Handed to the carrier. */
    SENT,
    /** In transit. */
    ON_THE_WAY,
    /** At the destination branch, awaiting delivery. */
    READY_TO_DELIVER,
    /** Delivered to the buyer. */
    DELIVERED,
    /** Waiting at a pickup point. */
    READY_TO_PICKUP,
    /** The pickup window elapsed. */
    PICKUP_TIME_EXPIRED,
    /** Returned to the seller. */
    RETURNED,
    /** Cancelled. */
    CANCELED,
    /** A carrier claim was opened. */
    CLAIMED,
    /** The carrier exposes no tracking for this parcel. */
    TRACKING_UNAVAILABLE,
    /** The carrier reported no usable state. */
    UNKNOWN,
    /** Processing failed; see {@link Parcel#errors()}. */
    ERROR,
    /** A delivery attempt failed. */
    DELIVERY_UNSUCCESSFUL,
    /** Redirected to another address or point. */
    REDIRECTED,
    /** A technical carrier state. */
    TECHNICAL,
    /** Tracking data is no longer available. */
    TRACKING_EXPIRED
}
