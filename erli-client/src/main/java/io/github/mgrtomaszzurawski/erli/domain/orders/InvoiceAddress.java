package io.github.mgrtomaszzurawski.erli.domain.orders;

import java.util.Optional;

/**
 * The address an invoice for the order is issued to. Which optional fields are populated depends on
 * {@link #type()}: a {@link InvoiceAddressType#COMPANY} invoice carries {@link #companyName()} and
 * {@link #taxIdentificationNumber()}, a {@link InvoiceAddressType#PERSON} one carries {@link #firstName()} and
 * {@link #lastName()}.
 *
 * <p><strong>Personal data.</strong> As with {@link DeliveryAddress}, {@link #toString()} is redacted.
 *
 * @param type           whether the invoice is for a company or a private person
 * @param address        the single-line address as Erli renders it
 * @param street         the street name
 * @param buildingNumber the building number
 * @param flatNumber     the flat number, when the address has one
 * @param postalCode            the postal code, in Polish {@code NN-NNN} form
 * @param city           the city
 * @param country        the country
 * @param firstName      the given name, for a private-person invoice
 * @param lastName       the family name, for a private-person invoice
 * @param companyName    the company name, for a company invoice
 * @param taxIdentificationNumber            the Polish tax identification number, for a company invoice
 */
public record InvoiceAddress(
        InvoiceAddressType type,
        String address,
        String street,
        String buildingNumber,
        Optional<String> flatNumber,
        String postalCode,
        String city,
        Country country,
        Optional<String> firstName,
        Optional<String> lastName,
        Optional<String> companyName,
        Optional<String> taxIdentificationNumber) {

    private static final String REDACTED_RENDERING = "InvoiceAddress[REDACTED]";

    /** Redacted: this record is buyer personal data and must not reach a log through a string. */
    @Override
    public String toString() {
        return REDACTED_RENDERING;
    }
}
