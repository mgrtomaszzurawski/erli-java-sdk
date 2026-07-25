package io.github.mgrtomaszzurawski.erli.domain.inbox;

/** Why the buyer returned part of an order. Mirrors the API enum, whose meanings the spec spells out. */
public enum ReturnReason {
    /** The buyer changed their mind. */
    RESIGN,
    /** The buyer ordered by mistake. */
    MISTAKE,
    /** The item was faulty or damaged on arrival. */
    ITEMS_QUALITY,
    /** The item did not match its description. */
    ITEMS_DESCRIPTION,
    /** The item was damaged in transit. */
    DELIVERY_QUALITY,
    /** The item does not suit the buyer. */
    DOES_NOT_FIT,
    /** Part of the item was missing. */
    ITEM_IS_MISSING,
    /** The order arrived late. */
    NOT_DELIVERED_ON_TIME,
    /** The item was damaged but its packaging was not. */
    ITEM_DAMAGED,
    /** Both the item and its packaging were damaged. */
    ITEM_AND_PACKAGE_DAMAGED,
    OTHER
}
