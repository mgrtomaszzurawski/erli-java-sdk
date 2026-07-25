package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * A ledger line from the operator's transaction history — what the API returns for the
 * {@code return} operation type.
 *
 * <p>This is the operator's own view of the money flow, so it is broader than {@link Payment} and
 * {@link Payout}: it also covers fees, chargebacks and transfers. Almost every field is optional
 * because which ones are populated depends on {@link #type()}.
 *
 * <p>Amounts here carry their own {@link #currency()} on the wire and are expressed in major units,
 * unlike the grosze used elsewhere in the Finance domain.
 *
 * @param type                          the operator's transaction type, e.g. {@code PAYOUT}
 * @param status                        the operator's status for the line
 * @param creationDate                  when the operator created the line, as it reported it
 * @param eventDate                     when the underlying event happened, as the operator reported it
 * @param erliCreationDate              when Erli recorded the line
 * @param sortDate                      the date Erli sorts the history by
 * @param amount                        the transaction amount
 * @param amountWithFee                 the amount including the fee
 * @param fee                           the fee charged
 * @param commissionFee                 the commission portion of the fee
 * @param currency                      the currency of the amounts on this line
 * @param paymentId                     the related payment, when there is one
 * @param payoutId                      the related payout, when there is one
 * @param feeId                         the related fee record, when there is one
 * @param providerId                    the operator's own identifier for the line
 * @param orders                        the orders this line covers
 * @param refund                        the refund this line settles, when it settles one
 * @param customer                      the counterparty, when the operator supplied one
 * @param balanceSnapshot               the balance snapshot attached by the operator; free-form,
 *                                      because the spec declares no schema for it
 * @param hasExternalOperationAssigned  whether an external operation is linked to this line
 */
public record Transaction(
        Optional<String> type,
        Optional<String> status,
        Optional<String> creationDate,
        Optional<String> eventDate,
        Optional<OffsetDateTime> erliCreationDate,
        Optional<OffsetDateTime> sortDate,
        Optional<Money> amount,
        Optional<Money> amountWithFee,
        Optional<Money> fee,
        Optional<Money> commissionFee,
        Optional<String> currency,
        Optional<Long> paymentId,
        Optional<Long> payoutId,
        Optional<Long> feeId,
        Optional<String> providerId,
        List<TransactionOrder> orders,
        Optional<TransactionRefund> refund,
        Optional<TransactionCustomer> customer,
        Optional<Object> balanceSnapshot,
        boolean hasExternalOperationAssigned) implements PaymentOperation {

    public Transaction {
        orders = List.copyOf(orders);
    }
}
