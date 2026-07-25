package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The field a product search is sorted and cursored by. The cursor is the value of this field on the last item of the previous page, so the sort field and the cursor always travel together.
 */
public enum ProductSortField {

    /** The seller-assigned external id. */
    EXTERNAL_ID,

    /** The marketplace's numeric id. */
    MARKETPLACE_ID,

    /** The product name. */
    NAME,

    /** The EAN barcode. */
    EAN,

    /** The seller's SKU. */
    SKU,

    /** The creation timestamp. */
    CREATED,

    /** The last-update timestamp. */
    UPDATED,

    /** The archival timestamp. */
    ARCHIVED_AT
}
