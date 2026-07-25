package io.github.mgrtomaszzurawski.erli.domain.inbox;

/** The carrier a parcel is tracked with. Mirrors the API enum. */
public enum DeliveryVendor {
    INPOST,
    POCZTA_POLSKA,
    DHL,
    DPD,
    DTS,
    FEDEX,
    POCZTEX24,
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
    /** The seller delivers with their own transport. */
    OWN_TRANSPORT,
    /** The buyer collects the order in person. */
    SELF_PICKUP,
    OTHER
}
