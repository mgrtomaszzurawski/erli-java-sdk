package io.github.mgrtomaszzurawski.erli.domain.inbox;

/**
 * Whether an invoice is issued to a company or to a private person. Decides which of the
 * {@link InvoiceAddress} identity fields the API populates.
 */
public enum InvoiceAddressType {
    /** Issued to a company: {@code companyName} and {@code nip} are populated. */
    COMPANY,
    /** Issued to a private person: {@code firstName} and {@code lastName} are populated. */
    PERSON
}
