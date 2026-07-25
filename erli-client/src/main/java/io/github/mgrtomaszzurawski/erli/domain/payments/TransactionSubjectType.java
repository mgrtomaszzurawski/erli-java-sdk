package io.github.mgrtomaszzurawski.erli.domain.payments;

/** What a transaction line was actually paying for. */
public enum TransactionSubjectType {

    /** A marketplace order. */
    ORDER,
    /** A top-up of the advertising balance. */
    ADS_PAY_IN,
    /** A debt-collection case. */
    VINDICATION_CASE,
    /** Parcels sent back by the buyer. */
    RETURN_PARCELS,
    /** A return the seller did not charge for. */
    FREE_RETURN,
    /** A deposit held as security. */
    DEPOSIT_FUND
}
