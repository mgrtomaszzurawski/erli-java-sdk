package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * The marketplace-side lifecycle of an order, as Erli sees it. Distinct from {@link SellerStatus},
 * which is the status the seller maintains in their own system and is the only one the SDK can write.
 */
public enum OrderStatus {

    /** Placed but not yet paid for. */
    PENDING,

    /** Paid for; the seller is expected to fulfil it. */
    PURCHASED,

    /** Cancelled; an order in this state can no longer be updated. */
    CANCELLED,

    /** Returned by the buyer. */
    RETURNED
}
