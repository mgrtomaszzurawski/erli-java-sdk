package io.github.mgrtomaszzurawski.erli.domain.inbox;

/** Status of the order in the seller's own system. Mirrors the API enum, including its {@code unknown}. */
public enum SellerStatus {
    CREATED,
    CANCELED,
    READY_TO_PROCESS,
    IN_PROGRESS,
    SENT,
    READY_TO_PICKUP,
    RECEIVED,
    RETURNED,
    RETURNING_TO_SENDER,
    UNKNOWN
}
