package io.github.mgrtomaszzurawski.erli.domain.inbox;

/** Operator of a pickup point. Mirrors the API enum. */
public enum PickupProvider {
    /** Erli's own pickup-point network ({@code pp}). */
    ERLI_PICKUP_POINT,
    INPOST,
    RUCH,
    DPD,
    UPS,
    DHL
}
