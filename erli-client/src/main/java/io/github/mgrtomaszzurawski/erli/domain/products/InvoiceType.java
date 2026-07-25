package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The kind of invoice the seller issues for a product.
 */
public enum InvoiceType {

    /** A standard VAT invoice. */
    VAT_INVOICE("vatInvoice"),

    /** A VAT invoice under the margin scheme. */
    VAT_INVOICE_WITH_MARGIN_SCHEME("vatInvoiceWithMarginScheme"),

    /** An invoice issued without VAT. */
    INVOICE_WITHOUT_VAT("invoiceWithoutVat"),

    /** No invoice is issued. */
    WITHOUT_INVOICE("withoutInvoice");

    private final String wireName;

    InvoiceType(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
