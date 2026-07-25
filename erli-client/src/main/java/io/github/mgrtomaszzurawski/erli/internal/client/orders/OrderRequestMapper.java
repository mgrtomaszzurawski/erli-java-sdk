package io.github.mgrtomaszzurawski.erli.internal.client.orders;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.SellerStatus;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderFilter;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderSearchRequest;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderUpdateRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderFilterAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderFilterAnyOf2;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderFilterAnyOf3;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderFilterAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderFilterAnyOfValue;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderSearch;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderSearchPagination;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderSearchPaginationAfter;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUpdate;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUpdateStatus;

/**
 * Maps the public request records of {@code domain.orders} onto the generated Layer-1 request bodies.
 * The reverse direction lives in {@link OrderMapper}. Internal: never exported.
 */
final class OrderRequestMapper {

    private OrderRequestMapper() {
    }

    /**
     * Build one {@code POST /orders/_search} body. The cursor argument wins over the request's own
     * {@link OrderSearchRequest#startAfter()}: the first page uses the request's cursor (if any) and
     * every later page uses the one the previous page ended on.
     */
    static OrderSearch toSearchBody(OrderSearchRequest request, Cursor after) {
        OrderSearchPagination pagination = new OrderSearchPagination()
                .sortField(toSortField(request.sortField()))
                .order(toSortOrder(request.direction()))
                .limit(request.pageSize());
        Cursor effectiveCursor = after != null ? after : request.startAfter().orElse(null);
        if (effectiveCursor != null) {
            pagination.after(new OrderSearchPaginationAfter(effectiveCursor.value()));
        }
        OrderSearch body = new OrderSearch().pagination(pagination);
        request.filter().ifPresent(filter -> body.setFilter(toRawFilter(filter)));
        return body;
    }

    static OrderUpdate toUpdateBody(OrderUpdateRequest update) {
        OrderUpdate body = new OrderUpdate();
        update.externalOrderId().ifPresent(body::setExternalOrderId);
        return body;
    }

    static OrderUpdateStatus toStatusBody(SellerStatus status) {
        return new OrderUpdateStatus().status(toRawSellerStatus(status));
    }

    // --- filters ----------------------------------------------------------------------------------

    /**
     * Translates the sealed filter tree onto Erli's recursive {@code anyOf} shape.
     *
     * <p>Written as an {@code instanceof} chain rather than a pattern {@code switch} because the
     * project's Java baseline is 17, where switch patterns are still a preview feature. The trailing
     * throw covers the case of a new permitted subtype being added without being mapped here.
     */
    private static io.github.mgrtomaszzurawski.erli.rest.model.OrderFilter toRawFilter(OrderFilter filter) {
        if (filter instanceof OrderFilter.Comparison comparison) {
            return new io.github.mgrtomaszzurawski.erli.rest.model.OrderFilter(
                    new OrderFilterAnyOf()
                            .field(toComparisonField(comparison.field()))
                            .operator(toComparisonOperator(comparison.operator()))
                            .value(new OrderFilterAnyOfValue(comparison.value())));
        }
        if (filter instanceof OrderFilter.Membership membership) {
            return new io.github.mgrtomaszzurawski.erli.rest.model.OrderFilter(
                    new OrderFilterAnyOf1()
                            .field(toMembershipField(membership.field()))
                            .operator(toMembershipOperator(membership.operator()))
                            .value(membership.values()));
        }
        if (filter instanceof OrderFilter.Combination combination) {
            return new io.github.mgrtomaszzurawski.erli.rest.model.OrderFilter(
                    new OrderFilterAnyOf2()
                            .operator(toLogicalOperator(combination.operator()))
                            .value(combination.operands().stream()
                                    .map(OrderRequestMapper::toRawFilter)
                                    .toList()));
        }
        if (filter instanceof OrderFilter.Negation negation) {
            return new io.github.mgrtomaszzurawski.erli.rest.model.OrderFilter(
                    new OrderFilterAnyOf3()
                            .operator(OrderFilterAnyOf3.OperatorEnum.NOT)
                            .value(toRawFilter(negation.operand())));
        }
        throw new IllegalStateException("Unmapped OrderFilter kind: " + filter.getClass().getName());
    }

    private static OrderFilterAnyOf.FieldEnum toComparisonField(OrderFilter.Field field) {
        return switch (field) {
            case ID -> OrderFilterAnyOf.FieldEnum.ID;
            case CREATED -> OrderFilterAnyOf.FieldEnum.CREATED;
            case UPDATED -> OrderFilterAnyOf.FieldEnum.UPDATED;
            case PAYMENT_STATUS -> OrderFilterAnyOf.FieldEnum.PAYMENT_STATUS;
            case USER_EMAIL -> OrderFilterAnyOf.FieldEnum.USER_EMAIL;
        };
    }

    private static OrderFilterAnyOf1.FieldEnum toMembershipField(OrderFilter.Field field) {
        return switch (field) {
            case ID -> OrderFilterAnyOf1.FieldEnum.ID;
            case CREATED -> OrderFilterAnyOf1.FieldEnum.CREATED;
            case UPDATED -> OrderFilterAnyOf1.FieldEnum.UPDATED;
            case PAYMENT_STATUS -> OrderFilterAnyOf1.FieldEnum.PAYMENT_STATUS;
            case USER_EMAIL -> OrderFilterAnyOf1.FieldEnum.USER_EMAIL;
        };
    }

    private static OrderFilterAnyOf.OperatorEnum toComparisonOperator(OrderFilter.ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL -> OrderFilterAnyOf.OperatorEnum.EQUAL;
            case NOT_EQUAL -> OrderFilterAnyOf.OperatorEnum.NOT_EQUAL;
            case GREATER_THAN -> OrderFilterAnyOf.OperatorEnum.GREATER_THAN;
            case GREATER_THAN_OR_EQUAL -> OrderFilterAnyOf.OperatorEnum.GREATER_THAN_OR_EQUAL_TO;
            case LESS_THAN -> OrderFilterAnyOf.OperatorEnum.LESS_THAN;
            case LESS_THAN_OR_EQUAL -> OrderFilterAnyOf.OperatorEnum.LESS_THAN_OR_EQUAL_TO;
        };
    }

    private static OrderFilterAnyOf1.OperatorEnum toMembershipOperator(OrderFilter.MembershipOperator operator) {
        return switch (operator) {
            case IN -> OrderFilterAnyOf1.OperatorEnum.IN;
            case NOT_IN -> OrderFilterAnyOf1.OperatorEnum.NIN;
        };
    }

    private static OrderFilterAnyOf2.OperatorEnum toLogicalOperator(OrderFilter.LogicalOperator operator) {
        return switch (operator) {
            case AND -> OrderFilterAnyOf2.OperatorEnum.AND;
            case OR -> OrderFilterAnyOf2.OperatorEnum.OR;
        };
    }

    // --- pagination and status --------------------------------------------------------------------

    private static OrderSearchPagination.SortFieldEnum toSortField(OrderSearchRequest.SortField sortField) {
        return switch (sortField) {
            case CREATED -> OrderSearchPagination.SortFieldEnum.CREATED;
            case UPDATED -> OrderSearchPagination.SortFieldEnum.UPDATED;
        };
    }

    private static OrderSearchPagination.OrderEnum toSortOrder(OrderSearchRequest.SortDirection direction) {
        return switch (direction) {
            case ASCENDING -> OrderSearchPagination.OrderEnum.ASC;
            case DESCENDING -> OrderSearchPagination.OrderEnum.DESC;
        };
    }

    private static OrderUpdateStatus.StatusEnum toRawSellerStatus(SellerStatus status) {
        return switch (status) {
            case CREATED -> OrderUpdateStatus.StatusEnum.CREATED;
            case CANCELED -> OrderUpdateStatus.StatusEnum.CANCELED;
            case READY_TO_PROCESS -> OrderUpdateStatus.StatusEnum.READY_TO_PROCESS;
            case IN_PROGRESS -> OrderUpdateStatus.StatusEnum.IN_PROGRESS;
            case SENT -> OrderUpdateStatus.StatusEnum.SENT;
            case READY_TO_PICKUP -> OrderUpdateStatus.StatusEnum.READY_TO_PICKUP;
            case RECEIVED -> OrderUpdateStatus.StatusEnum.RECEIVED;
            case RETURNED -> OrderUpdateStatus.StatusEnum.RETURNED;
            case RETURNING_TO_SENDER -> OrderUpdateStatus.StatusEnum.RETURNING_TO_SENDER;
            case UNKNOWN -> OrderUpdateStatus.StatusEnum.UNKNOWN;
        };
    }
}
