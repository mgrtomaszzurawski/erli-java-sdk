package io.github.mgrtomaszzurawski.erli.internal.client.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.payments.Market;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSortField;
import io.github.mgrtomaszzurawski.erli.domain.payments.PayoutSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.PayoutSortField;
import io.github.mgrtomaszzurawski.erli.domain.payments.ReturnSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.SortOrder;
import io.github.mgrtomaszzurawski.erli.internal.client.finance.MinorUnits;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Builds the {@code POST /payments/operations/_search} body from a domain search. Internal.
 */
final class PaymentSearchMapper {

    static final String TYPE_PAYMENT = "payment";
    static final String TYPE_PAYOUT = "payout";
    static final String TYPE_RETURN = "return";

    private static final String SORT_ASCENDING = "ASC";
    private static final String SORT_DESCENDING = "DESC";
    private static final String AMOUNT_FIELD = "amount";
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private PaymentSearchMapper() {
    }

    static OperationSearchBody toPaymentBody(PaymentSearch search, Cursor after) {
        OperationSearchBody.Pagination pagination = new OperationSearchBody.Pagination()
                .sortField(wireName(search.sortField()))
                .order(wireOrder(search.order()))
                .limit(search.pageSize())
                .after(cursorValue(after, search.sortField() == PaymentSortField.ID));

        OperationSearchBody body = new OperationSearchBody(TYPE_PAYMENT).pagination(pagination);
        search.filter().ifPresent(comparison -> body.filter(new OperationSearchBody.Comparison()
                .field(wireName(comparison.field()))
                .operator(wireOperator(comparison.operator()))
                .value(comparisonValue(comparison.operator(), comparison.value()))));
        return body;
    }

    static OperationSearchBody toPayoutBody(PayoutSearch search, Cursor after) {
        boolean numericCursor = search.sortField() != PayoutSortField.CREATED_AT;
        OperationSearchBody.Pagination pagination = new OperationSearchBody.Pagination()
                .sortField(wireName(search.sortField()))
                .order(wireOrder(search.order()))
                .limit(search.pageSize())
                .after(cursorValue(after, numericCursor));

        OperationSearchBody body = new OperationSearchBody(TYPE_PAYOUT).pagination(pagination);
        search.filter().ifPresent(comparison -> body.filter(new OperationSearchBody.Comparison()
                .field(wireName(comparison.field()))
                .operator(wireOperator(comparison.operator()))
                .value(comparisonValue(comparison.operator(), comparison.value()))));
        return body;
    }

    static OperationSearchBody toReturnBody(ReturnSearch search, int pageNumber) {
        OperationSearchBody body = new OperationSearchBody(TYPE_RETURN)
                .eventDateFrom(DAY_FORMAT.format(search.eventDateFrom()))
                .eventDateTo(DAY_FORMAT.format(search.eventDateTo()))
                .page(pageNumber)
                .perPage(search.pageSize())
                .market(wireMarket(search.market()))
                .types(search.types());
        search.creationDateFrom().map(PaymentSearchMapper::day).ifPresent(body::creationDateFrom);
        search.creationDateTo().map(PaymentSearchMapper::day).ifPresent(body::creationDateTo);
        return body;
    }

    private static String day(LocalDate date) {
        return DAY_FORMAT.format(date);
    }

    /**
     * The API compares the cursor against the sort field itself, so it must be sent as the same JSON
     * type: a number for id/amount sorts, an ISO timestamp string for date sorts.
     */
    private static Object cursorValue(Cursor after, boolean numeric) {
        if (after == null) {
            return null;
        }
        return numeric ? Long.valueOf(after.value()) : after.value();
    }

    /** A comparison operator takes one value; {@code in}/{@code nin} take the whole list. */
    private static Object comparisonValue(PaymentSearch.ComparisonOperator operator, List<Object> values) {
        List<Object> wireValues = values.stream().map(PaymentSearchMapper::wireValue).toList();
        boolean multiValued = operator == PaymentSearch.ComparisonOperator.IN
                || operator == PaymentSearch.ComparisonOperator.NOT_IN;
        return multiValued ? wireValues : wireValues.get(0);
    }

    private static Object wireValue(Object value) {
        if (value instanceof OffsetDateTime timestamp) {
            return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp);
        }
        if (value instanceof Money money) {
            // Payout amounts are compared in grosze, matching how the API stores them.
            return MinorUnits.toGrosze(money, AMOUNT_FIELD);
        }
        return value;
    }

    private static String wireOrder(SortOrder order) {
        return order == SortOrder.DESCENDING ? SORT_DESCENDING : SORT_ASCENDING;
    }

    private static String wireName(PaymentSortField sortField) {
        return switch (sortField) {
            case ID -> "id";
            case CREATED_AT -> "createdAt";
            case COMPLETED_AT -> "completedAt";
        };
    }

    private static String wireName(PayoutSortField sortField) {
        return switch (sortField) {
            case ID -> "id";
            case CREATED_AT -> "createdAt";
            case AMOUNT -> AMOUNT_FIELD;
        };
    }

    private static String wireName(PaymentSearch.PaymentFilterField field) {
        return switch (field) {
            case ID -> "id";
            case ORDER_ID -> "orderId";
            case CREATED_AT -> "createdAt";
            case COMPLETED_AT -> "completedAt";
        };
    }

    private static String wireName(PayoutSearch.PayoutFilterField field) {
        return switch (field) {
            case ID -> "id";
            case CREATED_AT -> "createdAt";
            case AMOUNT -> AMOUNT_FIELD;
        };
    }

    private static String wireOperator(PaymentSearch.ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL -> "=";
            case NOT_EQUAL -> "!=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case LESS_THAN -> "<";
            case LESS_THAN_OR_EQUAL -> "<=";
            case IN -> "in";
            case NOT_IN -> "nin";
        };
    }

    /** The marketplace discriminator; the API spells it lowercase. */
    private static String wireMarket(Market market) {
        return market.name().toLowerCase(Locale.ROOT);
    }
}
