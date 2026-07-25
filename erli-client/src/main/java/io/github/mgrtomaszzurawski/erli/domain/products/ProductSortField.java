package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The field a product search is sorted and cursored by. The cursor is the value of this field on the
 * last item of the previous page, so the sort field and the cursor always travel together.
 */
public enum ProductSortField {

    /** The seller-assigned external id (unique, so safe to cursor on). */
    EXTERNAL_ID("externalId", FilterValueKind.TEXT),

    /** The marketplace's numeric id (unique, so safe to cursor on). */
    MARKETPLACE_ID("marketplaceId", FilterValueKind.NUMBER),

    /** The product name. */
    NAME("name", FilterValueKind.TEXT),

    /** The EAN barcode. */
    EAN("ean", FilterValueKind.TEXT),

    /** The seller's SKU. */
    SKU("sku", FilterValueKind.TEXT),

    /** The creation timestamp. */
    CREATED("created", FilterValueKind.DATE_TIME),

    /** The last-update timestamp. */
    UPDATED("updated", FilterValueKind.DATE_TIME),

    /** The archival timestamp. */
    ARCHIVED_AT("archivedAt", FilterValueKind.DATE_TIME);

    private final String wireName;
    private final FilterValueKind cursorKind;

    ProductSortField(String wireName, FilterValueKind cursorKind) {
        this.wireName = wireName;
        this.cursorKind = cursorKind;
    }

    /**
     * The JSON type the {@code pagination.after} cursor takes for this sort field. The cursor is a value
     * of the sort field itself, so it carries that field's type — sending a numeric
     * {@link #MARKETPLACE_ID} cursor as a string would not compare and the walk would end after one page.
     */
    public FilterValueKind cursorKind() {
        return cursorKind;
    }

    /**
     * Whether this field is unique per product, and therefore safe to page a whole catalog by.
     *
     * <p>Erli's {@code pagination.after} is a <strong>strict</strong> bound — the server returns rows
     * whose sort value is greater than the cursor. If several products share the last row's value and
     * did not fit on the page, the next page starts past all of them and those products are never
     * returned. Only {@link #EXTERNAL_ID} and {@link #MARKETPLACE_ID} are unique; a name, an EAN or a
     * timestamp can repeat, and {@code updateAll} stamps the same {@code updated} on every product it
     * touches.
     *
     * <p>{@link ProductAccess#search} therefore refuses to continue a walk past the first page on a
     * non-unique sort, rather than dropping products silently. A single page is always delivered.
     */
    public boolean isUniquePerProduct() {
        return this == EXTERNAL_ID || this == MARKETPLACE_ID;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
