package io.github.mgrtomaszzurawski.erli.domain.payments;

/** Field a payment search is ordered by. */
public enum PaymentSortField {

    /** The payment id. */
    ID,
    /** When the payment was started. */
    CREATED_AT,
    /** When the payment finished. */
    COMPLETED_AT
}
