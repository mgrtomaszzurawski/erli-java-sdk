package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.SortOrder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * The query for {@code POST /products/_search}: what to match, what to return, and how to order it.
 *
 * <p>Callers normally do not set {@code after} — {@link ProductAccess#search(ProductSearchRequest)}
 * returns a lazy stream and walks the cursor itself. It is exposed for the rarer case of resuming an
 * interrupted walk from a cursor that was persisted.
 *
 * <pre>{@code
 * ProductSearchRequest query = ProductSearchRequest.builder()
 *         .filter(ProductFilter.equalTo(ProductFilterField.STATUS, "active"))
 *         .sortBy(ProductSortField.UPDATED, SortOrder.DESCENDING)
 *         .fields(Set.of(ProductField.NAME, ProductField.PRICE, ProductField.STOCK))
 *         .build();
 * }</pre>
 */
public final class ProductSearchRequest {

    /** The largest page Erli accepts. */
    public static final int MAX_PAGE_SIZE = 200;

    /** The page size Erli applies when the request does not state one. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    private final Optional<ProductFilter> filter;
    private final Set<ProductField> fields;
    private final ProductSortField sortField;
    private final SortOrder order;
    private final Optional<Integer> pageSize;
    private final Optional<Cursor> after;

    private ProductSearchRequest(Builder builder) {
        this.filter = builder.filter;
        this.fields = Collections.unmodifiableSet(builder.fields);
        this.sortField = builder.sortField;
        this.order = builder.order;
        this.pageSize = builder.pageSize;
        this.after = builder.after;
    }

    /** A search with no filter and the marketplace's defaults — every product, oldest external id first. */
    public static ProductSearchRequest all() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** What to match; empty matches everything. */
    public Optional<ProductFilter> filter() {
        return filter;
    }

    /** The projection; empty returns whole products. */
    public Set<ProductField> fields() {
        return fields;
    }

    /** The field results are ordered and cursored by. */
    public ProductSortField sortField() {
        return sortField;
    }

    /** The sort direction. */
    public SortOrder order() {
        return order;
    }

    /** The page size, when the caller pinned one. */
    public Optional<Integer> pageSize() {
        return pageSize;
    }

    /**
     * The page size actually sent — the caller's, or {@link #DEFAULT_PAGE_SIZE}. The SDK always states a
     * limit rather than letting the server apply its own, because the page walk decides it has reached
     * the end by comparing a page against the size it asked for.
     */
    public int effectivePageSize() {
        return pageSize.orElse(DEFAULT_PAGE_SIZE);
    }

    /** The cursor to resume after, when resuming a persisted walk. */
    public Optional<Cursor> after() {
        return after;
    }

    /** A copy of this request positioned after the given cursor, used to walk pages. */
    public ProductSearchRequest after(Cursor cursor) {
        return builder()
                .filter(filter.orElse(null))
                .fields(fields)
                .sortBy(sortField, order)
                .pageSize(pageSize.orElse(null))
                .after(cursor)
                .build();
    }

    /** Builder for {@link ProductSearchRequest}. */
    public static final class Builder {

        private Optional<ProductFilter> filter = Optional.empty();
        private Set<ProductField> fields = EnumSet.noneOf(ProductField.class);
        private ProductSortField sortField = ProductSortField.EXTERNAL_ID;
        private SortOrder order = SortOrder.ASCENDING;
        private Optional<Integer> pageSize = Optional.empty();
        private Optional<Cursor> after = Optional.empty();

        private Builder() {
        }

        /** Match only products satisfying this filter. */
        public Builder filter(ProductFilter value) {
            this.filter = Optional.ofNullable(value);
            return this;
        }

        /** Return only these fields. An empty set returns whole products. */
        public Builder fields(Set<ProductField> value) {
            this.fields = value == null || value.isEmpty()
                    ? EnumSet.noneOf(ProductField.class)
                    : EnumSet.copyOf(value);
            return this;
        }

        /**
         * Order by a field. The cursor is derived from this field, so changing it mid-walk would
         * invalidate the cursor — set it once, before the walk starts.
         */
        public Builder sortBy(ProductSortField field, SortOrder direction) {
            this.sortField = field == null ? ProductSortField.EXTERNAL_ID : field;
            this.order = direction == null ? SortOrder.ASCENDING : direction;
            return this;
        }

        /**
         * How many products per page.
         *
         * @throws IllegalArgumentException if outside 1..{@value #MAX_PAGE_SIZE}
         */
        public Builder pageSize(Integer value) {
            if (value != null && (value < 1 || value > MAX_PAGE_SIZE)) {
                throw new IllegalArgumentException(
                        "pageSize must be between 1 and " + MAX_PAGE_SIZE + ", was " + value);
            }
            this.pageSize = Optional.ofNullable(value);
            return this;
        }

        /** Resume after a previously observed cursor. */
        public Builder after(Cursor value) {
            this.after = Optional.ofNullable(value);
            return this;
        }

        public ProductSearchRequest build() {
            return new ProductSearchRequest(this);
        }
    }
}
