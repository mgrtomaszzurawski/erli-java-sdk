package io.github.mgrtomaszzurawski.erli.domain.inbox;

import io.github.mgrtomaszzurawski.erli.core.model.Buyer;
import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.Delivery;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryTracking;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.OrderReturn;
import io.github.mgrtomaszzurawski.erli.core.model.OrderStatus;
import io.github.mgrtomaszzurawski.erli.core.model.Rebate;
import io.github.mgrtomaszzurawski.erli.core.model.SellerStatus;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Payload of the three {@code ORDER_*} message types: a snapshot of the order at the moment the event
 * was raised. The API sends the same shape for all three, so which event it is comes from
 * {@link Message#type()}, not from the payload.
 *
 * <p>This is a <em>snapshot inside an event</em>, not a live order — read {@code client.orders()} for
 * the current state of the order identified by {@link #id()}.
 *
 * <p><strong>Buyer personal data.</strong> {@link #buyer()} carries the buyer's e-mail and address;
 * those records redact themselves in {@code toString()}, and this record inherits that behaviour by
 * delegating to them.
 *
 * @param id                    the marketplace's order id
 * @param externalOrderId       the order's id in the shop's own system, once the shop has assigned one
 * @param status                the marketplace-side status
 * @param buyer                 who placed the order — absent when the API withholds buyer data
 * @param lines                 the purchased positions
 * @param rebate                the discount applied, if any
 * @param delivery              how the order ships
 * @param comment               the buyer's note to the seller, if any
 * @param totalPrice            what the buyer paid in total
 * @param currency              the order's currency
 * @param deliveryTracking      parcel tracking, once shipping starts
 * @param payment               the payment summary the event carries
 * @param returns               returns registered against the order
 * @param calculatedParcelsCount how many parcels the marketplace calculated for the order
 * @param sellerStatus          the status in the seller's own system
 * @param created               when the order was created
 * @param updated               when the order last changed
 * @param purchasedAt           when the order was paid for, once it has been
 * @param cursor                this order's paging cursor, usable as {@code pagination.after}
 */
public record OrderEvent(
        OrderId id,
        Optional<String> externalOrderId,
        OrderStatus status,
        Optional<Buyer> buyer,
        List<OrderLine> lines,
        Optional<Rebate> rebate,
        Delivery delivery,
        Optional<String> comment,
        Money totalPrice,
        Currency currency,
        Optional<DeliveryTracking> deliveryTracking,
        @SuppressWarnings("deprecation") Optional<OrderPaymentSummary> payment,
        List<OrderReturn> returns,
        Optional<Integer> calculatedParcelsCount,
        SellerStatus sellerStatus,
        OffsetDateTime created,
        OffsetDateTime updated,
        Optional<OffsetDateTime> purchasedAt,
        Optional<Cursor> cursor) implements MessagePayload {

    public OrderEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(externalOrderId, "externalOrderId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(buyer, "buyer");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(rebate, "rebate");
        Objects.requireNonNull(delivery, "delivery");
        Objects.requireNonNull(comment, "comment");
        Objects.requireNonNull(totalPrice, "totalPrice");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(deliveryTracking, "deliveryTracking");
        Objects.requireNonNull(payment, "payment");
        Objects.requireNonNull(returns, "returns");
        Objects.requireNonNull(calculatedParcelsCount, "calculatedParcelsCount");
        Objects.requireNonNull(sellerStatus, "sellerStatus");
        Objects.requireNonNull(created, "created");
        Objects.requireNonNull(updated, "updated");
        Objects.requireNonNull(purchasedAt, "purchasedAt");
        Objects.requireNonNull(cursor, "cursor");
        lines = List.copyOf(lines);
        returns = List.copyOf(returns);
    }
}
