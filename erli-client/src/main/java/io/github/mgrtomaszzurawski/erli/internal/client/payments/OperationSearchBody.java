package io.github.mgrtomaszzurawski.erli.internal.client.payments;

import java.util.List;

/**
 * The request body for {@code POST /payments/operations/_search}.
 *
 * <p>Hand-written rather than reused from Layer 1, because the generated models cannot express what
 * the live endpoint actually accepts (see {@code KNOWN-SERVER-BEHAVIORS.md}):
 * <ul>
 *   <li>the spec declares {@code type} as a <em>query</em> parameter, but the server reads it from
 *       the <em>body</em> and returns {@code 400 "type is required"} when it is only in the query —
 *       so no generated request model has the field at all;</li>
 *   <li>the spec types the body as an <em>array</em> of a {@code oneOf}, while the server wants a
 *       single object;</li>
 *   <li>{@code SearchTransactions} types its dates as {@code date-time}, but the server accepts only
 *       {@code yyyy-mm-dd} and rejects every ISO timestamp form.</li>
 * </ul>
 *
 * <p>Serialized by Jackson through the getters; unset fields are omitted, never sent as null.
 * Internal: never exported.
 */
public final class OperationSearchBody {

    private final String type;
    private Pagination pagination;
    private Comparison filter;
    private String eventDateFrom;
    private String eventDateTo;
    private String creationDateFrom;
    private String creationDateTo;
    private Integer page;
    private Integer perPage;
    private List<String> types;
    private String market;

    public OperationSearchBody(String type) {
        this.type = type;
    }

    /** The operation type discriminator: {@code payment}, {@code payout} or {@code return}. */
    public String getType() {
        return type;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public OperationSearchBody pagination(Pagination value) {
        this.pagination = value;
        return this;
    }

    public Comparison getFilter() {
        return filter;
    }

    public OperationSearchBody filter(Comparison value) {
        this.filter = value;
        return this;
    }

    public String getEventDateFrom() {
        return eventDateFrom;
    }

    public OperationSearchBody eventDateFrom(String value) {
        this.eventDateFrom = value;
        return this;
    }

    public String getEventDateTo() {
        return eventDateTo;
    }

    public OperationSearchBody eventDateTo(String value) {
        this.eventDateTo = value;
        return this;
    }

    public String getCreationDateFrom() {
        return creationDateFrom;
    }

    public OperationSearchBody creationDateFrom(String value) {
        this.creationDateFrom = value;
        return this;
    }

    public String getCreationDateTo() {
        return creationDateTo;
    }

    public OperationSearchBody creationDateTo(String value) {
        this.creationDateTo = value;
        return this;
    }

    public Integer getPage() {
        return page;
    }

    public OperationSearchBody page(Integer value) {
        this.page = value;
        return this;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public OperationSearchBody perPage(Integer value) {
        this.perPage = value;
        return this;
    }

    public List<String> getTypes() {
        return types;
    }

    public OperationSearchBody types(List<String> value) {
        this.types = value == null || value.isEmpty() ? null : List.copyOf(value);
        return this;
    }

    public String getMarket() {
        return market;
    }

    public OperationSearchBody market(String value) {
        this.market = value;
        return this;
    }

    /** The cursor-pagination block shared by the payment and payout searches. */
    public static final class Pagination {

        private String sortField;
        private String order;
        private Integer limit;
        private Object after;

        public String getSortField() {
            return sortField;
        }

        public Pagination sortField(String value) {
            this.sortField = value;
            return this;
        }

        public String getOrder() {
            return order;
        }

        public Pagination order(String value) {
            this.order = value;
            return this;
        }

        public Integer getLimit() {
            return limit;
        }

        public Pagination limit(Integer value) {
            this.limit = value;
            return this;
        }

        /**
         * The cursor to continue after. Typed as {@link Object} because the API expects a number when
         * sorting by id or amount and an ISO timestamp when sorting by date.
         */
        public Object getAfter() {
            return after;
        }

        public Pagination after(Object value) {
            this.after = value;
            return this;
        }
    }

    /** A single {@code field}/{@code operator}/{@code value} comparison. */
    public static final class Comparison {

        private String field;
        private String operator;
        private Object value;

        public String getField() {
            return field;
        }

        public Comparison field(String value) {
            this.field = value;
            return this;
        }

        public String getOperator() {
            return operator;
        }

        public Comparison operator(String value) {
            this.operator = value;
            return this;
        }

        /** A scalar for a comparison operator, or a list for {@code in}/{@code nin}. */
        public Object getValue() {
            return value;
        }

        public Comparison value(Object newValue) {
            this.value = newValue;
            return this;
        }
    }
}
