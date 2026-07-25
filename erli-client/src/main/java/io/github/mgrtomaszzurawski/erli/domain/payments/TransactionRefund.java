package io.github.mgrtomaszzurawski.erli.domain.payments;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;

import java.util.Optional;

/**
 * The refund a transaction settles.
 *
 * @param id      the refund's identifier
 * @param orderId the order being refunded
 * @param shop    the external shop the refund belongs to, when the API attributes one; free-form,
 *                because the spec declares no schema for it
 */
public record TransactionRefund(Optional<Long> id, Optional<OrderId> orderId, Optional<Object> shop) {
}
