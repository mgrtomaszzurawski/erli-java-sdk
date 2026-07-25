package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * The operator of the pickup point chosen for a delivery.
 */
public enum PickupProvider {

    /** Erli's own {@code punkt partnerski} network. */
    PP,

    INPOST,
    RUCH,
    DPD,
    UPS,
    DHL
}
