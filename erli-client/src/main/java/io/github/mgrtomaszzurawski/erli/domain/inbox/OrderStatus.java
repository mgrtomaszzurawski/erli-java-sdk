package io.github.mgrtomaszzurawski.erli.domain.inbox;

/** Marketplace-side status of the order carried by an {@link OrderEvent}. Mirrors the API enum. */
public enum OrderStatus {
    PENDING,
    PURCHASED,
    CANCELLED,
    RETURNED
}
