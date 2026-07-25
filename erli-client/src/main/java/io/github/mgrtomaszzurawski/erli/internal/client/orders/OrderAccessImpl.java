package io.github.mgrtomaszzurawski.erli.internal.client.orders;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.domain.orders.Order;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderAccess;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderSearchRequest;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderUpdateRequest;
import io.github.mgrtomaszzurawski.erli.domain.orders.SellerStatus;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.CursorPagination;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.Page;
import io.github.mgrtomaszzurawski.erli.internal.PathTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * {@link OrderAccess} implementation: calls the four {@code /orders} operations through the shared
 * {@link HttpRuntime} and maps between the domain records and the Layer-1 bodies. Internal.
 */
public final class OrderAccessImpl implements OrderAccess {

    /** Name of the {@code {id}} placeholder in the templated paths in {@link ApiPaths}. */
    private static final String ID_PARAMETER = "id";

    // The Layer-1 order model shares its simple name with the domain record, so the response types are
    // bound once here rather than fully qualifying them at every call site.
    private static final Class<io.github.mgrtomaszzurawski.erli.rest.model.Order> RAW_ORDER =
            io.github.mgrtomaszzurawski.erli.rest.model.Order.class;
    private static final Class<io.github.mgrtomaszzurawski.erli.rest.model.Order[]> RAW_ORDER_PAGE =
            io.github.mgrtomaszzurawski.erli.rest.model.Order[].class;

    private final HttpRuntime runtime;

    public OrderAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public Stream<Order> search(OrderSearchRequest request) {
        Objects.requireNonNull(request, "request");
        return CursorPagination.stream(after -> fetchPage(request, after));
    }

    @Override
    public Order byId(OrderId orderId) {
        return OrderMapper.toDomain(runtime.get(withId(ApiPaths.ORDER_BY_ID, orderId), RAW_ORDER));
    }

    @Override
    public void update(OrderId orderId, OrderUpdateRequest update) {
        Objects.requireNonNull(update, "update");
        if (update.isEmpty()) {
            throw new IllegalArgumentException(
                    "OrderUpdateRequest would change nothing; set at least one field before updating");
        }
        runtime.patch(withId(ApiPaths.ORDER_BY_ID, orderId),
                OrderRequestMapper.toUpdateBody(update), Void.class);
    }

    @Override
    public void changeStatus(OrderId orderId, SellerStatus status) {
        Objects.requireNonNull(status, "status");
        runtime.patch(withId(ApiPaths.ORDER_STATUS, orderId),
                OrderRequestMapper.toStatusBody(status), Void.class);
    }

    /**
     * Fetch one page of a search.
     *
     * <p>{@code POST /orders/_search} answers with a bare JSON array rather than an envelope, so the
     * cursor for the next page is not a top-level field: it is the {@code cursor} carried by the last
     * order on this page. An empty page therefore ends the walk, which is also what
     * {@link CursorPagination} treats as terminal.
     *
     * <p><strong>A page whose last order has no {@code cursor} also ends the walk.</strong> The cursor
     * is the only continuation token the endpoint offers, so without one there is nothing to ask for;
     * the spec marks it optional, which leaves "there is no more data" and "the server omitted it"
     * indistinguishable. Ending is the only behaviour available, but it does mean a caller could in
     * principle receive a short result set that looks complete.
     *
     * <p>Page size deliberately is <em>not</em> used to tell those apart. Neither the spec nor the
     * official guide promises a full page while more data exists, so treating a short page as the tail
     * would be inventing a server contract — and getting it wrong would truncate silently in the other
     * direction. Whether a live server ever omits the cursor mid-walk is recorded as an open question
     * in {@code KNOWN-SERVER-BEHAVIORS.md}, to settle during the Phase 3 sweep on seeded data.
     */
    private Page<Order> fetchPage(OrderSearchRequest request, Cursor after) {
        var body = OrderRequestMapper.toSearchBody(request, after);
        io.github.mgrtomaszzurawski.erli.rest.model.Order[] rawOrders =
                runtime.post(ApiPaths.ORDERS_SEARCH, body, RAW_ORDER_PAGE);
        if (rawOrders == null || rawOrders.length == 0) {
            return new Page<>(List.of(), null);
        }
        List<Order> orders = Arrays.stream(rawOrders).map(OrderMapper::toDomain).toList();
        Cursor nextCursor = orders.get(orders.size() - 1).cursor().orElse(null);
        return new Page<>(orders, nextCursor);
    }

    /**
     * Substitute the order id into a templated path. The core {@link PathTemplate} percent-encodes it
     * as a single path segment — an id is consumer-supplied data and must not be able to alter the
     * request's path.
     */
    private static String withId(String template, OrderId orderId) {
        Objects.requireNonNull(orderId, "orderId");
        return PathTemplate.expand(template, Map.of(ID_PARAMETER, orderId.value()));
    }
}
