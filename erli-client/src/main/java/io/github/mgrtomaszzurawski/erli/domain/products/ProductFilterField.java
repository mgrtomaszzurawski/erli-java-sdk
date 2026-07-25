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
    EXTERNAL_ID(true, true),

    /** The marketplace's numeric id. Ordered comparisons and membership. */
    MARKETPLACE_ID(true, true),

    /** The product name. Ordered comparisons and membership. */
    NAME(true, true),

    /** The EAN barcode. Ordered comparisons and membership. */
    EAN(true, true),

    /** The seller's SKU. Ordered comparisons and membership. */
    SKU(true, true),

    /** The creation timestamp. Ordered comparisons and membership. */
    CREATED(true, true),

    /** The last-update timestamp. Ordered comparisons and membership. */
    UPDATED(true, true),

    /** The archival timestamp. Ordered comparisons and membership. */
    ARCHIVED_AT(true, true),

    /** The selling price, in grosze as the API states it. Ordered comparisons and membership. */
    PRICE(true, true),

    /** Units available. Ordered comparisons and membership. */
    STOCK(true, true),

    /** The marketplace category id. Ordered comparisons and membership. */
    CATEGORY_ID(true, true),

    /** The VAT rate. Ordered comparisons and membership. */
    TAX_RATE(true, true),

    /** An external reference id. Equality and membership only. */
    EXTERNAL_REFERENCE_ID(false, true),

    /** Whether the product is offered for sale. Equality only. */
    STATUS(false, false),

    /** Whether the product is archived. Equality only. */
    ARCHIVED(false, false);

    private final boolean ordered;
    private final boolean membership;

    ProductFilterField(boolean ordered, boolean membership) {
        this.ordered = ordered;
        this.membership = membership;
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
