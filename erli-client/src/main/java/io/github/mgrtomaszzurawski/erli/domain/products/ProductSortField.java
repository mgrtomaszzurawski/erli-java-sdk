package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The field a product search is sorted and cursored by. The cursor is the value of this field on the last item of the previous page, so the sort field and the cursor always travel together.
 */
public enum ProductSortField {

    /** The seller-assigned external id (unique, so safe to cursor on). */
    EXTERNAL_ID("externalId"),

    /** The marketplace's numeric id (unique, so safe to cursor on). */
    MARKETPLACE_ID("marketplaceId"),

    /** The product name. */
    NAME("name"),

    /** The EAN barcode. */
    EAN("ean"),

    /** The seller's SKU. */
    SKU("sku"),

    /** The creation timestamp. */
    CREATED("created"),

    /** The last-update timestamp. */
    UPDATED("updated"),

    /** The archival timestamp. */
    ARCHIVED_AT("archivedAt");

    private final String wireName;

    ProductSortField(String wireName) {
        this.wireName = wireName;
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
     * <p>{@link ProductAccess#search} warns once when a walk crosses a page boundary on a non-unique
     * sort, rather than dropping products silently.
     */
    public boolean isUniquePerProduct() {
        return this == EXTERNAL_ID || this == MARKETPLACE_ID;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
