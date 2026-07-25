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
    EXTERNAL_ID(true),

    /** The marketplace's numeric id. Ordered comparisons and membership. */
    MARKETPLACE_ID(true),

    /** The product name. Ordered comparisons and membership. */
    NAME(true),

    /** The EAN barcode. Ordered comparisons and membership. */
    EAN(true),

    /** The seller's SKU. Ordered comparisons and membership. */
    SKU(true),

    /** The creation timestamp. Ordered comparisons and membership. */
    CREATED(true),

    /** The last-update timestamp. Ordered comparisons and membership. */
    UPDATED(true),

    /** The archival timestamp. Ordered comparisons and membership. */
    ARCHIVED_AT(true),

    /** The selling price, in grosze as the API states it. Ordered comparisons and membership. */
    PRICE(true),

    /** Units available. Ordered comparisons and membership. */
    STOCK(true),

    /** The marketplace category id. Ordered comparisons and membership. */
    CATEGORY_ID(true),

    /** The VAT rate. Ordered comparisons and membership. */
    TAX_RATE(true),

    /** An external reference id. Equality and membership only. */
    EXTERNAL_REFERENCE_ID(false),

    /** Whether the product is offered for sale. Equality and membership only. */
    STATUS(false),

    /** Whether the product is archived. Equality and membership only. */
    ARCHIVED(false);

    private final boolean ordered;

    ProductFilterField(boolean ordered) {
        this.ordered = ordered;
    }

    /** Whether this field supports the ordered comparisons, not just equality. */
    public boolean supportsOrderedComparison() {
        return ordered;
    }
}
