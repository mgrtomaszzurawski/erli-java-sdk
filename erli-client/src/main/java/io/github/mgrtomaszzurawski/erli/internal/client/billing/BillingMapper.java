package io.github.mgrtomaszzurawski.erli.internal.client.billing;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntry;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntryFilter;
import io.github.mgrtomaszzurawski.erli.domain.billing.RebateOrigin;
import io.github.mgrtomaszzurawski.erli.internal.client.finance.MinorUnits;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesRequestPagination;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesRequestSimpleFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesResponseInnerRebateOriginInner;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps between the {@code billing} domain records and the generated Layer-1 models. Internal.
 */
final class BillingMapper {

    private static final String FIELD_ID = "id";
    private static final String FIELD_OCCURRED_AT = "occurredAt";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_AMOUNT = "amount";
    private static final String FIELD_BALANCE_AFTER = "balanceAfter";
    private static final String FIELD_REBATE_REASON = "rebateReason";
    private static final String RAW_ENTRY_NAME = "raw BillingEntriesResponseInner";

    private BillingMapper() {
    }

    /**
     * Domain filter to the wire request, positioned after {@code cursor}.
     *
     * <p>The API supports exactly one sort — {@code id} descending — so the request always states it
     * explicitly rather than relying on the documented defaults.
     */
    static BillingEntriesRequest toRaw(BillingEntryFilter filter, Cursor cursor) {
        Objects.requireNonNull(filter, "filter");
        BillingEntriesRequestPagination pagination = new BillingEntriesRequestPagination()
                .sortField(BillingEntriesRequestPagination.SortFieldEnum.ID)
                .order(BillingEntriesRequestPagination.OrderEnum.DESC)
                .limit(filter.pageSize());
        if (cursor != null) {
            pagination.after(Integer.valueOf(cursor.value()));
        }

        BillingEntriesRequestSimpleFilter simpleFilter = new BillingEntriesRequestSimpleFilter();
        filter.type().ifPresent(simpleFilter::type);
        filter.fromOccurredAt().ifPresent(simpleFilter::fromOccurredAt);
        filter.toOccurredAt().ifPresent(simpleFilter::toOccurredAt);
        filter.orderId().map(OrderId::value).ifPresent(simpleFilter::orderId);
        filter.shopId().map(Math::toIntExact).ifPresent(simpleFilter::shopId);
        filter.productId().map(Math::toIntExact).ifPresent(simpleFilter::productId);

        return new BillingEntriesRequest().pagination(pagination).simpleFilter(simpleFilter);
    }

    static BillingEntry toDomain(BillingEntriesResponseInner rawEntry) {
        Objects.requireNonNull(rawEntry, RAW_ENTRY_NAME);
        return new BillingEntry(
                require(rawEntry.getId(), FIELD_ID).longValue(),
                require(rawEntry.getOccurredAt(), FIELD_OCCURRED_AT),
                require(rawEntry.getType(), FIELD_TYPE),
                require(rawEntry.getDescription(), FIELD_DESCRIPTION),
                MinorUnits.fromGrosze(require(rawEntry.getAmount(), FIELD_AMOUNT)),
                MinorUnits.fromGrosze(require(rawEntry.getBalanceAfter(), FIELD_BALANCE_AFTER)),
                Optional.ofNullable(rawEntry.getOrderId()).map(OrderId::of),
                Optional.ofNullable(rawEntry.getShopId()).map(Integer::longValue),
                Optional.ofNullable(rawEntry.getProductId()).map(Integer::longValue),
                toRebateOrigins(rawEntry.getRebateOrigin()));
    }

    private static List<RebateOrigin> toRebateOrigins(
            List<BillingEntriesResponseInnerRebateOriginInner> rawOrigins) {
        if (rawOrigins == null) {
            return List.of();
        }
        return rawOrigins.stream().map(BillingMapper::toRebateOrigin).toList();
    }

    private static RebateOrigin toRebateOrigin(BillingEntriesResponseInnerRebateOriginInner rawOrigin) {
        return new RebateOrigin(
                MinorUnits.fromGrosze(require(rawOrigin.getAmount(), FIELD_AMOUNT)),
                require(rawOrigin.getRebateReason(), FIELD_REBATE_REASON));
    }

    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(
                    "Billing entry is missing the required '" + fieldName + "' field");
        }
        return value;
    }
}
