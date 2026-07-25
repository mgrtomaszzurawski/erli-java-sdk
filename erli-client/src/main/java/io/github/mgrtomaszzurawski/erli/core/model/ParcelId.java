package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Identifier of a shipping parcel. Owned by core; shared by Shipping &amp; Delivery and Orders.
 *
 * @param value the non-blank parcel id
 */
public record ParcelId(String value) {

    public ParcelId {
        value = Identifiers.requireText(value, "ParcelId");
    }

    public static ParcelId of(String value) {
        return new ParcelId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
