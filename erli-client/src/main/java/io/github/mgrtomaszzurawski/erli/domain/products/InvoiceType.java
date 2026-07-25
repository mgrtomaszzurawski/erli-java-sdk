package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The kind of invoice the seller issues for a product.
 */
public enum InvoiceType {

    /** A standard VAT invoice. */
    VAT_INVOICE,

    /** A VAT invoice under the margin scheme. */
    VAT_INVOICE_WITH_MARGIN_SCHEME,

    /** An invoice issued without VAT. */
    INVOICE_WITHOUT_VAT,

    /** No invoice is issued. */
    WITHOUT_INVOICE
}
