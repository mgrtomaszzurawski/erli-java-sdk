package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * The carrier a parcel was handed to, as reported on {@link DeliveryTracking}. Erli's list mixes
 * couriers with two non-carrier options ({@link #OWN_TRANSPORT}, {@link #SELF_PICKUP}) and a catch-all
 * {@link #OTHER}.
 */
public enum ShippingVendor {

    INPOST,
    POCZTA_POLSKA,
    POCZTEX_24,
    DHL,
    DPD,
    DTS,
    FEDEX,
    RHENUS,
    RABEN,
    GLS,
    UPS,
    RUCH,
    ORLEN,
    GEIS,
    PATRON_SERVICE,
    PEKAES,
    TNT_EXPRESS,
    SCHENKER,
    AMBRO_EXPRESS,
    DSV,
    JAS_FBG,
    ROHLIG_SUUS,
    HELLMANN,

    /** Delivered by the seller's own transport. */
    OWN_TRANSPORT,

    /** Collected in person by the buyer. */
    SELF_PICKUP,

    /** A carrier Erli does not enumerate. */
    OTHER
}
