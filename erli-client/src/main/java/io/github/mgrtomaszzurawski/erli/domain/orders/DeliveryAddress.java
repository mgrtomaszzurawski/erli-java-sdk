package io.github.mgrtomaszzurawski.erli.domain.orders;

import java.util.Optional;

/**
 * Where the buyer wants the order delivered.
 *
 * <p><strong>Personal data.</strong> Every field here identifies a natural person, so
 * {@link #toString()} is overridden to render nothing but the type name. Accessors return the real
 * values — the redaction guards accidental disclosure through logging, not deliberate use.
 *
 * @param firstName      the buyer's given name
 * @param lastName       the buyer's family name
 * @param companyName    the company, when the parcel goes to a business address
 * @param address        the single-line address as Erli renders it
 * @param street         the street name
 * @param buildingNumber the building number
 * @param flatNumber     the flat number, when the address has one
 * @param postalCode            the postal code, in Polish {@code NN-NNN} form
 * @param city           the city
 * @param country        the country
 * @param phone          the contact phone number, nine digits
 */
public record DeliveryAddress(
        String firstName,
        String lastName,
        Optional<String> companyName,
        String address,
        String street,
        String buildingNumber,
        Optional<String> flatNumber,
        String postalCode,
        String city,
        Country country,
        String phone) {

    private static final String REDACTED_RENDERING = "DeliveryAddress[REDACTED]";

    /** Redacted: this record is buyer personal data and must not reach a log through a string. */
    @Override
    public String toString() {
        return REDACTED_RENDERING;
    }
}
