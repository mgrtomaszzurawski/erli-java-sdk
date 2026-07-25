package io.github.mgrtomaszzurawski.erli.domain.shipping;

/** The parcel attributes {@code POST /shipping/parcels/_search} can filter on. */
public enum ParcelSearchField {

    /** The numeric parcel id; compared as a number. */
    ID("id"),
    /** The order id; compared lexicographically. */
    ORDER_ID("orderId"),
    /** Last modification timestamp; compared chronologically. */
    UPDATED_AT("updatedAt");

    private final String wireValue;

    ParcelSearchField(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this field is sent as on the wire. */
    public String wireValue() {
        return wireValue;
    }
}
