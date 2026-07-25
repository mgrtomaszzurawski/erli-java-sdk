package io.github.mgrtomaszzurawski.erli.domain.inbox;

/** Status of the order's payment as reported inside an order event. Mirrors the API enum. */
public enum PaymentStatus {
    NEW,
    PENDING,
    WAITING_FOR_CONFIRMATION,
    COMPLETED,
    CANCELED
}
