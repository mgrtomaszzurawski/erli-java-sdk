package io.github.mgrtomaszzurawski.erli.domain.payments;

/**
 * One entry in the shop's money flow. The API calls all three "payment operations" and selects
 * between them with a {@code type} discriminator, so this sealed interface mirrors that: a
 * {@link Payment} in from a buyer, a {@link Payout} out to the seller, or a {@link Transaction}
 * ledger line for a return.
 *
 * <p>Sealed, so a {@code switch} over the three is exhaustive without a default branch. It declares
 * no members deliberately: the three shapes share no field the API guarantees on all of them — a
 * {@link Transaction} in particular carries no identifier of its own, only references to the
 * payment, payout or fee it relates to.
 */
public sealed interface PaymentOperation permits Payment, Payout, Transaction {
}
