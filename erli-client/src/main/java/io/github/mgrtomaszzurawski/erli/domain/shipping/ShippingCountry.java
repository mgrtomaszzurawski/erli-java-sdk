package io.github.mgrtomaszzurawski.erli.domain.shipping;

/**
 * Destination country supported by Erli shipping. The API defaults an omitted country to
 * {@link #PL} for domestic parcels.
 */
public enum ShippingCountry {

    /** Poland. */
    PL,
    /** Germany. */
    DE
}
