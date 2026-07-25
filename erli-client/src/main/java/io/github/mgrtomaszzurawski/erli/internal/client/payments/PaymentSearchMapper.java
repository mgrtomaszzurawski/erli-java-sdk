package io.github.mgrtomaszzurawski.erli.internal.client.payments;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.Market;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
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

/**
 * Builds the {@code POST /payments/operations/_search} body from a domain search. Internal.
 */
final class PaymentSearchMapper {

    static final String TYPE_PAYMENT = "payment";
    static final String TYPE_PAYOUT = "payout";
    static final String TYPE_RETURN = "return";

    private static final String SORT_ASCENDING = "ASC";
    private static final String SORT_DESCENDING = "DESC";

    // Wire names for the sortable/filterable fields, exactly as the API spells them.
    private static final String FIELD_ID = "id";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_COMPLETED_AT = "completedAt";
    private static final String FIELD_ORDER_ID = "orderId";
    private static final String AMOUNT_FIELD = "amount";

    // Wire spellings of the comparison operators.
    private static final String OPERATOR_EQUAL = "=";
    private static final String OPERATOR_NOT_EQUAL = "!=";
    private static final String OPERATOR_GREATER_THAN = ">";
    private static final String OPERATOR_GREATER_THAN_OR_EQUAL = ">=";
    private static final String OPERATOR_LESS_THAN = "<";
    private static final String OPERATOR_LESS_THAN_OR_EQUAL = "<=";
    private static final String OPERATOR_IN = "in";
    private static final String OPERATOR_NOT_IN = "nin";
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
        // The record's constructor already guarantees a single value for a scalar operator.
        return operator.multiValued() ? wireValues : wireValues.get(0);
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
            case ID -> FIELD_ID;
            case CREATED_AT -> FIELD_CREATED_AT;
            case COMPLETED_AT -> FIELD_COMPLETED_AT;
        };
    }

    private static String wireName(PayoutSortField sortField) {
        return switch (sortField) {
            case ID -> FIELD_ID;
            case CREATED_AT -> FIELD_CREATED_AT;
            case AMOUNT -> AMOUNT_FIELD;
        };
    }

    private static String wireName(PaymentSearch.PaymentFilterField field) {
        return switch (field) {
            case ID -> FIELD_ID;
            case ORDER_ID -> FIELD_ORDER_ID;
            case CREATED_AT -> FIELD_CREATED_AT;
            case COMPLETED_AT -> FIELD_COMPLETED_AT;
        };
    }

    private static String wireName(PayoutSearch.PayoutFilterField field) {
        return switch (field) {
            case ID -> FIELD_ID;
            case CREATED_AT -> FIELD_CREATED_AT;
            case AMOUNT -> AMOUNT_FIELD;
        };
    }

    private static String wireOperator(PaymentSearch.ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL -> OPERATOR_EQUAL;
            case NOT_EQUAL -> OPERATOR_NOT_EQUAL;
            case GREATER_THAN -> OPERATOR_GREATER_THAN;
            case GREATER_THAN_OR_EQUAL -> OPERATOR_GREATER_THAN_OR_EQUAL;
            case LESS_THAN -> OPERATOR_LESS_THAN;
            case LESS_THAN_OR_EQUAL -> OPERATOR_LESS_THAN_OR_EQUAL;
            case IN -> OPERATOR_IN;
            case NOT_IN -> OPERATOR_NOT_IN;
        };
    }

    /** The marketplace discriminator; the API spells it lowercase. */
    private static String wireMarket(Market market) {
        return market.wireValue();
    }
}
