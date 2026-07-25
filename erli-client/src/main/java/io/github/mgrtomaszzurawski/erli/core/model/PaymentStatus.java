package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Lifecycle of a buyer payment.
 *
 * <p>Deliberately has <strong>no</strong> {@code UNRECOGNIZED} constant. This is a closed lifecycle
 * that callers branch on to decide whether money has actually arrived, so a state this SDK cannot
 * model is refused at mapping time rather than folded into a sentinel a caller might read as
 * "not settled". See the SDK's unknown-enum policy (CORE-12): growing enums take a sentinel, closed
 * ones stay fail-loud.
 */
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
