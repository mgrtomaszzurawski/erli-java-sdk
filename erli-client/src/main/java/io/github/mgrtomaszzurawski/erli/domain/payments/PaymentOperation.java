package io.github.mgrtomaszzurawski.erli.domain.payments;

/**
 * One entry in the shop's money flow. The API calls all three "payment operations" and selects
 * between them with a {@code type} discriminator, so this sealed interface mirrors that: a
 * {@link Payment} in from a buyer, a {@link Payout} out to the seller, or a {@link Transaction}
 * ledger line for a return.
 *
 * <p>Sealed, so the set of cases is closed and cannot grow behind your back. It declares
 * no members deliberately: the three shapes share no field the API guarantees on all of them — a
 * {@link Transaction} in particular carries no identifier of its own, only references to the
 * payment, payout or fee it relates to.
 *
 * <p>No method returns this type — each search is typed to its own result so nothing has to be
 * downcast. It exists for the other direction: <em>combining</em> them, when you want one timeline
 * of everything that moved money.
 *
 * <pre>{@code
 * List<PaymentOperation> timeline = new ArrayList<>();
 * client.payments().searchPayments(PaymentSearch.all()).forEach(timeline::add);
 * client.payments().searchPayouts(PayoutSearch.all()).forEach(timeline::add);
 *
 * for (PaymentOperation operation : timeline) {
 *     if (operation instanceof Payment payment) {
 *         System.out.println("in  " + payment.amount());
 *     } else if (operation instanceof Payout payout) {
 *         System.out.println("out " + payout.amount());
 *     }
 * }
 * }</pre>
 *
 * <p>On Java 21+ the same dispatch can be written as an exhaustive {@code switch} with no default
 * branch; pattern switch is still a preview feature on this SDK's Java 17 baseline.
 */
public sealed interface PaymentOperation permits Payment, Payout, Transaction {
}
