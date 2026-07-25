package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Whether an invoice is issued to a company or to a private person. The type decides which of the
 * optional invoice-address fields Erli populates.
 */
public enum InvoiceAddressType {

    /** Issued to a business — the company name and tax identification number are set. */
    COMPANY,

    /** Issued to a private person — the first and last name are set. */
    PERSON
}
