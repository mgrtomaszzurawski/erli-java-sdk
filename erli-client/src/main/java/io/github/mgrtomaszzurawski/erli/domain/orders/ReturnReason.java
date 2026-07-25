package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * Why the buyer returned an order. Erli requires exactly one reason per return.
 */
public enum ReturnReason {

    /** The buyer changed their mind. */
    RESIGN,

    /** The buyer bought it by mistake. */
    MISTAKE,

    /** The item was faulty or damaged. */
    ITEMS_QUALITY,

    /** The item did not match its description. */
    ITEMS_DESCRIPTION,

    /** The item was damaged in transit. */
    DELIVERY_QUALITY,

    /** The item does not suit the buyer. */
    DOES_NOT_FIT,

    /** Part of the item was missing. */
    ITEM_IS_MISSING,

    /** The parcel arrived late. */
    NOT_DELIVERED_ON_TIME,

    /** The item was damaged but the packaging was not. */
    ITEM_DAMAGED,

    /** Both the item and its packaging were damaged. */
    ITEM_AND_PACKAGE_DAMAGED,

    /** A reason Erli does not enumerate. */
    OTHER
}
