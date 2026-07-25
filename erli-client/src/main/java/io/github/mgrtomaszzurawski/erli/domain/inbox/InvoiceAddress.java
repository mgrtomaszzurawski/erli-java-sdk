package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.Objects;
import java.util.Optional;

/**
 * Where the invoice for the order should be issued. Which identity fields are populated depends on
 * {@link #type()}: a company invoice carries {@link #companyName()} and {@link #nip()}, a personal one
 * carries {@link #firstName()} and {@link #lastName()}.
 *
 * <p><strong>Buyer personal data.</strong> {@link #toString()} reports only the invoice type and the
 * non-identifying location fields; identity and street are redacted.
 *
 * @param type           whether the invoice goes to a company or a person
 * @param address        the address as one line, as the API formats it
 * @param street         the street name
 * @param buildingNumber the building number
 * @param flatNumber     the flat number, if any
 * @param zip            the postal code, formatted {@code NN-NNN}
 * @param city           the city
 * @param country        the country
 * @param firstName      the given name, for a personal invoice
 * @param lastName       the family name, for a personal invoice
 * @param companyName    the company name, for a company invoice
 * @param nip            the Polish tax identification number, for a company invoice
 */
public record InvoiceAddress(
        InvoiceAddressType type,
        String address,
        String street,
        String buildingNumber,
        Optional<String> flatNumber,
        String zip,
        String city,
        Country country,
        Optional<String> firstName,
        Optional<String> lastName,
        Optional<String> companyName,
        Optional<String> nip) {

    private static final String REDACTED = "<redacted>";

    public InvoiceAddress {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(buildingNumber, "buildingNumber");
        Objects.requireNonNull(flatNumber, "flatNumber");
        Objects.requireNonNull(zip, "zip");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(companyName, "companyName");
        Objects.requireNonNull(nip, "nip");
    }

    @Override
    public String toString() {
        return "InvoiceAddress[type=" + type + ", city=" + city + ", zip=" + zip + ", country=" + country
                + ", identity=" + REDACTED + ", street=" + REDACTED + "]";
    }
}
