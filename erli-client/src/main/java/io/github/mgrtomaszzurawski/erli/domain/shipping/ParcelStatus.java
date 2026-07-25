package io.github.mgrtomaszzurawski.erli.domain.shipping;

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
    TRACKING_EXPIRED("trackingExpired"),

    /**
     * A status this release does not know. Erli's parcel statuses come from carrier integrations and
     * grow as carriers are added, so a status minted after this SDK was built must not be able to fail
     * a whole read: one unrecognised value in a 200-parcel search would otherwise cost the caller every
     * other parcel. Branch on it if you need to; {@link #wireValue()} returns an empty string.
     *
     * <p>Note this constant does <em>not</em> carry the value Erli actually sent — the SDK exposes no
     * raw payload on a successful read, so that value is lost. The Orders bucket answers the same
     * question with a value object that round-trips the unknown string verbatim; aligning the two is a
     * fleet decision, filed as CORE-15.
     */
    UNRECOGNIZED("");

    private static final Map<String, ParcelStatus> BY_WIRE = Stream.of(values())
            .filter(status -> status != UNRECOGNIZED)
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
     * Resolve a wire string to a status, or {@link #UNRECOGNIZED} if this release does not know it.
     *
     * <p>Deliberately tolerant, unlike the smaller enums in this package. Those describe closed
     * vocabularies where an unknown value means something is genuinely wrong; parcel status is an open,
     * carrier-driven vocabulary where a new value is routine, and failing the read would punish the
     * caller for Erli shipping with a new carrier.
     *
     * <p>The shared codec decodes an unknown enum as {@code null} before a mapper sees it (see
     * {@code KNOWN-SERVER-BEHAVIORS.md}), so tolerance here only helps if the mappers let that
     * {@code null} through instead of guarding it — {@code ParcelMapper}/{@code ExternalParcelMapper}
     * do, deliberately, and say so. The consequence worth knowing: a genuinely <em>absent</em> status is
     * indistinguishable from an unknown one after the codec, so it also reads as {@link #UNRECOGNIZED}
     * rather than naming a contract break.
     */
    public static ParcelStatus fromWire(String wireValue) {
        if (wireValue == null) {
            return UNRECOGNIZED;
        }
        return BY_WIRE.getOrDefault(wireValue, UNRECOGNIZED);
    }
}
