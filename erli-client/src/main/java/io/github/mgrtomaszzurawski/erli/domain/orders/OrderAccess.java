package io.github.mgrtomaszzurawski.erli.domain.orders;

import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.SellerStatus;
import java.util.stream.Stream;

/**
 * Read and update the shop's orders. Reached via {@code client.orders()}.
 *
 * <p>Public surface: consumers import this package and {@code core}, never anything internal.
 */
public interface OrderAccess {

    /**
     * Find orders matching a request, as a lazy stream over every matching page.
     *
     * <p>Pages are fetched as the stream is consumed, so a short-circuiting terminal operation costs
     * only the pages it actually reads — {@code search(request).limit(10)} performs a single request.
     * The stream is sequential and ordered; consume it before closing the client.
     *
     * <p>The walk continues from the {@link Order#cursor()} of the last order on each page, and stops
     * when a page comes back empty or its last order carries no cursor — the cursor being the only
     * continuation token the endpoint offers. Keep the last cursor you processed and pass it to
     * {@link OrderSearchRequest.Builder#startAfter} to resume later.
     *
     * @param request what to look for, how to sort it, and how large a page to fetch
     * @return a lazy stream over the matching orders, in the requested order
     */
    Stream<Order> search(OrderSearchRequest request);

    /**
     * Fetch one order by its Erli id.
     *
     * @param orderId the order to fetch
     * @return the order
     * @throws ErliNotFoundException if the shop has no such order
     */
    Order byId(OrderId orderId);

    /**
     * Apply a partial update to an order.
     *
     * <p>A no-op request is rejected rather than sent, so a caller cannot silently issue a write that
     * changes nothing.
     *
     * @param orderId the order to update
     * @param update  the changes to apply
     * @throws IllegalArgumentException if {@code update} would change nothing
     * @throws ErliNotFoundException    if the shop has no such order
     * @throws ErliValidationException  if the order is cancelled, and so can no longer be updated
     */
    void update(OrderId orderId, OrderUpdateRequest update);

    /**
     * Move the order to a new status in the seller's own system — the seller-side workflow signal that
     * an order has been picked, packed, dispatched or collected.
     *
     * <p>This writes {@link Order#sellerStatus()}. The marketplace-side {@link Order#status()} is
     * Erli's own and cannot be set through the API.
     *
     * @param orderId the order to move
     * @param status  the status to move it to
     * @throws ErliNotFoundException   if the shop has no such order
     * @throws ErliValidationException if Erli rejects the transition
     */
    void changeStatus(OrderId orderId, SellerStatus status);
}
