package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import java.util.List;
import java.util.Optional;

/**
 * One order covered by a transaction, with the lines that made it up.
 *
 * @param orderId       the order this part of the transaction relates to
 * @param subjectType   what was actually being paid for
 * @param deliveryPrice the delivery charge on this order
 * @param items         the order lines
 * @param shop          the external shop, when the API attributes one; free-form, because the spec
 *                      declares no schema for it
 * @param lockedFund    funds held rather than released, when present; free-form for the same reason
 */
public record TransactionOrder(
        Optional<OrderId> orderId,
        Optional<TransactionSubjectType> subjectType,
        Optional<Money> deliveryPrice,
        List<TransactionOrderItem> items,
        Optional<Object> shop,
        Optional<Object> lockedFund) {

    public TransactionOrder {
        items = List.copyOf(items);
    }
}
