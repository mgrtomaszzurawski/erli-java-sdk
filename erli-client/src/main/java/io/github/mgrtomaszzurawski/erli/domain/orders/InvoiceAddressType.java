package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * Whether an invoice is issued to a company or to a private person. The type decides which of the
 * optional {@link InvoiceAddress} fields Erli populates.
 */
public enum InvoiceAddressType {

    /** Issued to a business — {@link InvoiceAddress#companyName()} and {@link InvoiceAddress#nip()} are set. */
    COMPANY,

    /** Issued to a private person — {@link InvoiceAddress#firstName()} and {@link InvoiceAddress#lastName()} are set. */
    PERSON
}
