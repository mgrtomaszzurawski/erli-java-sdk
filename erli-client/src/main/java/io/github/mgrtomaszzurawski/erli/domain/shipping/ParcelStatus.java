package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lifecycle status of a parcel, from creation through delivery, return or failure.
 *
 * <p>The same vocabulary is shared by Erli-handled parcels ({@code /shipping/parcels}) and externally
 * shipped ones ({@code /shipping/external}); the external write endpoints accept only the subset a
 * seller can set by hand. The spec repeats this enum inline in five places, so the SDK maps by the wire
 * string via {@link #fromWire} rather than one hand-written switch per occurrence — and
 * {@link #wireValue()} gives the reverse direction the write endpoints need.
 */
public enum ParcelStatus {

    /** Created in the SDK or panel, not yet handed to the carrier. */
    PREPARING("preparing"),
    /** Ready to be handed over. */
    READY_TO_SEND("readyToSend"),
    /** Waiting for the courier pickup. */
    WAITING_FOR_COURIER("waitingForCourier"),
    /** Handed to the carrier. */
    SENT("sent"),
    /** In transit. */
    ON_THE_WAY("onTheWay"),
    /** At the destination branch, awaiting delivery. */
    READY_TO_DELIVER("readyToDeliver"),
    /** Delivered to the buyer. */
    DELIVERED("delivered"),
    /** Waiting at a pickup point. */
    READY_TO_PICKUP("readyToPickup"),
    /** The pickup window elapsed. */
    PICKUP_TIME_EXPIRED("pickupTimeExpired"),
    /** Returned to the seller. */
    RETURNED("returned"),
    /** Cancelled. */
    CANCELED("canceled"),
    /** A carrier claim was opened. */
    CLAIMED("claimed"),
    /** The carrier exposes no tracking for this parcel. */
    TRACKING_UNAVAILABLE("trackingUnavailable"),
    /** The carrier reported no usable state. */
    UNKNOWN("unknown"),
    /** Processing failed; see {@link Parcel#errors()}. */
    ERROR("error"),
    /** A delivery attempt failed. */
    DELIVERY_UNSUCCESSFUL("deliveryUnsuccessful"),
    /** Redirected to another address or point. */
    REDIRECTED("redirected"),
    /** A technical carrier state. */
    TECHNICAL("technical"),
    /** Tracking data is no longer available. */
    TRACKING_EXPIRED("trackingExpired");

    private static final Map<String, ParcelStatus> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ParcelStatus::wireValue, Function.identity()));

    private final String wireValue;

    ParcelStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this status is sent and received as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a status.
     *
     * <p>On the live path this receives the wire value of an already-decoded Layer-1 enum. Note the
     * shared codec decodes an unknown enum value as {@code null} rather than throwing, so a status Erli
     * adds after the vendored spec does not reach here at all: a required field then fails in the
     * mapper naming the field, and an optional one reads as absent. This guard therefore fires only if
     * this domain enum drifts out of sync with the generated one.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static ParcelStatus fromWire(String wireValue) {
        ParcelStatus status = BY_WIRE.get(wireValue);
        if (status == null) {
            throw new ErliTransportException(
                    "No ParcelStatus constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return status;
    }
}
