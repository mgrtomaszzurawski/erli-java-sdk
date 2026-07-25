package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A product field a search may filter on.
 *
 * <p>Not every field works with every operator, and the marketplace is strict about it: ordered
 * comparisons ({@code >}, {@code <=}, …) apply to the scalar fields only, while
 * {@link ProductFilterField#STATUS}, {@link ProductFilterField#ARCHIVED} and
 * {@link ProductFilterField#EXTERNAL_REFERENCE_ID} accept equality alone. {@link ProductFilter} checks
 * the pairing when the filter is built, so an unsupported combination fails locally rather than as a 400.
 */
public enum ProductFilterField {

    /** The seller-assigned external id. Ordered comparisons and membership. */
    EXTERNAL_ID("externalId", true, true, FilterValueKind.TEXT),

    /** The marketplace's numeric id. Ordered comparisons and membership. */
    MARKETPLACE_ID("marketplaceId", true, true, FilterValueKind.NUMBER),

    /** The product name. Ordered comparisons and membership. */
    NAME("name", true, true, FilterValueKind.TEXT),

    /** The EAN barcode. Ordered comparisons and membership. */
    EAN("ean", true, true, FilterValueKind.TEXT),

    /** The seller's SKU. Ordered comparisons and membership. */
    SKU("sku", true, true, FilterValueKind.TEXT),

    /** The creation timestamp. Ordered comparisons and membership. */
    CREATED("created", true, true, FilterValueKind.DATE_TIME),

    /** The last-update timestamp. Ordered comparisons and membership. */
    UPDATED("updated", true, true, FilterValueKind.DATE_TIME),

    /** The archival timestamp. Ordered comparisons and membership. */
    ARCHIVED_AT("archivedAt", true, true, FilterValueKind.DATE_TIME),

    /** The selling price, in grosze as the API states it. Ordered comparisons and membership. */
    PRICE("price", true, true, FilterValueKind.NUMBER),

    /** Units available. Ordered comparisons and membership. */
    STOCK("stock", true, true, FilterValueKind.NUMBER),

    /** The marketplace category id. Ordered comparisons and membership. */
    CATEGORY_ID("categoryId", true, true, FilterValueKind.NUMBER),

    /** The VAT rate. Ordered comparisons and membership. */
    TAX_RATE("taxRate", true, true, FilterValueKind.TEXT),

    /** An external reference id. Equality and membership only. */
    EXTERNAL_REFERENCE_ID("externalReferenceId", false, true, FilterValueKind.TEXT),

    /** Whether the product is offered for sale. Equality only. */
    STATUS("status", false, false, FilterValueKind.TEXT),

    /** Whether the product is archived. Equality only. */
    ARCHIVED("archived", false, false, FilterValueKind.BOOLEAN);

    private final String wireName;
    private final boolean ordered;
    private final boolean membership;
    private final FilterValueKind valueKind;

    ProductFilterField(String wireName, boolean ordered, boolean membership, FilterValueKind valueKind) {
        this.wireName = wireName;
        this.ordered = ordered;
        this.membership = membership;
        this.valueKind = valueKind;
    }

    /** The value the Erli API uses for this field on the wire. */
    public String wireName() {
        return wireName;
    }

    /**
     * The JSON type this field's filter value takes. Erli types a filter value per field and rejects a
     * number sent as a string, so the SDK converts the caller's text before it reaches the wire.
     */
    public FilterValueKind valueKind() {
        return valueKind;
    }

    /** Whether this field supports the ordered comparisons, not just equality. */
    public boolean supportsOrderedComparison() {
        return ordered;
    }

    /**
     * Whether this field can be tested against a set with {@code in}/{@code nin}. Erli's membership
     * branch omits {@link #STATUS} and {@link #ARCHIVED}, which accept equality alone.
     */
    public boolean supportsMembership() {
        return membership;
    }
}
