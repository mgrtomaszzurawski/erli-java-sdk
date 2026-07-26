package io.github.mgrtomaszzurawski.erli.domain.orders;

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
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A single order, as returned by {@code GET /orders/{id}} and by each page of
 * {@code POST /orders/_search}.
 *
 * <p>Two statuses travel together and mean different things: {@link #status()} is the marketplace's
 * view and is read-only, while {@link #sellerStatus()} is the seller's own and is the one
 * {@link OrderAccess#changeStatus} writes.
 *
 * <p>Erli sends monetary amounts as integer minor units (grosze) with the currency alongside; they are
 * exposed here as {@link Money} so no caller has to remember the scale.
 *
 * @param id                     Erli's order id
 * @param externalOrderId        the seller's own id for this order, once one has been set
 * @param status                 the marketplace-side status
 * @param buyer                  who ordered, and where it goes — absent on anonymised orders
 * @param items                  the purchased lines; never empty
 * @param rebate                 the order-wide discount, when one applied
 * @param delivery               how the order is to be delivered
 * @param comment                the buyer's note to the seller, when they left one
 * @param totalPrice             the order total, delivery included
 * @param deliveryTracking       parcel tracking, once the seller or carrier has reported any
 * @param payment                the embedded payment block — deprecated upstream
 * @param returns                returns opened against this order; empty when there are none
 * @param calculatedParcelsCount how many parcels Erli expects this order to ship in
 * @param sellerStatus           the status in the seller's own system
 * @param created                when the order was created
 * @param updated                when the order last changed
 * @param purchasedAt            when the order was paid for, once it has been
 * @param cursor                 this order's pagination cursor; see {@link OrderAccess#search}
 */
@SuppressWarnings("deprecation") // the deprecated OrderPayment component is mapped for lossless round-trip
public record Order(
        OrderId id,
        Optional<String> externalOrderId,
        OrderStatus status,
        Optional<Buyer> buyer,
        List<OrderItem> items,
        Optional<Rebate> rebate,
        Delivery delivery,
        Optional<String> comment,
        Money totalPrice,
        Optional<DeliveryTracking> deliveryTracking,
        Optional<OrderPayment> payment,
        List<OrderReturn> returns,
        OptionalInt calculatedParcelsCount,
        SellerStatus sellerStatus,
        OffsetDateTime created,
        OffsetDateTime updated,
        Optional<OffsetDateTime> purchasedAt,
        Optional<Cursor> cursor) {

    /** Defensively copies the collections so the record is genuinely immutable. */
    public Order {
        items = List.copyOf(items);
        returns = List.copyOf(returns);
    }

    /**
     * A log-safe rendering: order identity, state, totals and timestamps only.
     *
     * <p>The record's generated {@code toString} would recurse into everything, so the components that
     * carry personal data are omitted here by name rather than left to their own redaction. That
     * includes the obvious ones ({@link #buyer()}) but also two that are easy to miss: {@link
     * #comment()} and {@link OrderReturn#comment()} are buyer-authored free text, which is exactly
     * where a buyer writes a phone number, and {@link Delivery#pickupPlace()} carries a street address.
     *
     * <p>Reach for the accessors when you genuinely need those values; they are never redacted there.
     */
    @Override
    public String toString() {
        return "Order[id=" + id
                + ", status=" + status
                + ", sellerStatus=" + sellerStatus
                + ", totalPrice=" + totalPrice.amount() + " " + totalPrice.currency()
                + ", items=" + items.size()
                + ", returns=" + returns.size()
                + ", created=" + created
                + ", updated=" + updated
                + "]";
    }
}
