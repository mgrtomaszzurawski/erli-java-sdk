package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.Objects;
import java.util.Optional;

/**
 * Where the buyer wants the order delivered.
 *
 * <p><strong>Buyer personal data.</strong> {@link #toString()} reports only the fields that are not
 * personally identifying (city, zip, country); names, street and phone are redacted so an order event
 * cannot leak the buyer's address into a log. Read the components directly when you need the values.
 *
 * @param firstName      the buyer's given name
 * @param lastName       the buyer's family name
 * @param companyName    the company the parcel is addressed to, if any
 * @param address        the address as one line, as the API formats it
 * @param street         the street name
 * @param buildingNumber the building number
 * @param flatNumber     the flat number, if any
 * @param zip            the postal code, formatted {@code NN-NNN}
 * @param city           the city
 * @param country        the country
 * @param phone          the buyer's nine-digit phone number
 */
public record DeliveryAddress(
        String firstName,
        String lastName,
        Optional<String> companyName,
        String address,
        String street,
        String buildingNumber,
        Optional<String> flatNumber,
        String zip,
        String city,
        Country country,
        String phone) {

    private static final String REDACTED = "<redacted>";

    public DeliveryAddress {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(companyName, "companyName");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(buildingNumber, "buildingNumber");
        Objects.requireNonNull(flatNumber, "flatNumber");
        Objects.requireNonNull(zip, "zip");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(phone, "phone");
    }

    @Override
    public String toString() {
        return "DeliveryAddress[city=" + city + ", zip=" + zip + ", country=" + country
                + ", recipient=" + REDACTED + ", street=" + REDACTED + ", phone=" + REDACTED + "]";
    }
}
