package io.github.mgrtomaszzurawski.erli.domain.payments;

/** Lifecycle of a buyer payment. */
public enum PaymentStatus {

    /** Created but not yet started by the buyer. */
    NEW,
    /** Started and awaiting the operator's result. */
    PENDING,
    /** The operator has the funds but has not confirmed settlement. */
    WAITING_FOR_CONFIRMATION,
    /** Settled successfully. */
    COMPLETED,
    /** Abandoned or rejected. */
    CANCELED
}
