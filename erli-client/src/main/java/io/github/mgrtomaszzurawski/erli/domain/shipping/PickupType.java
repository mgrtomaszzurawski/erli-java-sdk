package io.github.mgrtomaszzurawski.erli.domain.shipping;

/** How the buyer receives the parcel: at their address, or at a carrier pickup point. */
public enum PickupType {

    /** Door-to-door courier delivery. */
    COURIER,
    /** Collection at a pickup point identified by {@link ShippingParty#pointCode()}. */
    POINT
}
