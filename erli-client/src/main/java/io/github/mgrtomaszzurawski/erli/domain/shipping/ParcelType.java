package io.github.mgrtomaszzurawski.erli.domain.shipping;

/**
 * Who carries out the shipment. Erli models the two kinds as separate resources with a single-valued
 * {@code type} discriminator each; the SDK exposes one enum so a caller can branch on it uniformly.
 */
public enum ParcelType {

    /** Shipped through Erli's own carrier integration ({@code /shipping/parcels}). */
    INTERNAL,
    /** Shipped by the seller outside Erli, tracked here only ({@code /shipping/external}). */
    EXTERNAL
}
