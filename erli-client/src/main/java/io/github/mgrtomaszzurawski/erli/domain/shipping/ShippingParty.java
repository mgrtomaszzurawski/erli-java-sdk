package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.util.Objects;
import java.util.Optional;

/**
 * One end of a shipment — the sender (the shop or its posting point) or the receiver (the buyer).
 * Erli uses the same address shape for both.
 *
 * <p><strong>Personal data.</strong> As a receiver this record holds the buyer's name, delivery
 * address, phone number and e-mail. {@link #toString()} is overridden to disclose presence but never
 * content, so a parcel that reaches a log line, a stack trace or a debugger snapshot does not leak
 * buyer data. Call the accessors explicitly to obtain the values.
 *
 * @param firstName     given name, when supplied
 * @param lastName      family name, when supplied
 * @param companyName   company name, when the party is a business
 * @param street        street name, when supplied
 * @param buildingNumber building number, when supplied
 * @param flatNumber    flat number, when the address has one
 * @param city          city, when supplied
 * @param zip           postcode, when supplied
 * @param country       destination country, when supplied ({@code pl} is assumed domestically)
 * @param phoneNumber   contact phone number, when supplied
 * @param email         contact e-mail, when supplied
 * @param pickupType    whether the parcel goes to the address or to a pickup point
 * @param pointCode     the pickup-point code, when {@link PickupType#POINT} applies
 */
public record ShippingParty(
        Optional<String> firstName,
        Optional<String> lastName,
        Optional<String> companyName,
        Optional<String> street,
        Optional<String> buildingNumber,
        Optional<String> flatNumber,
        Optional<String> city,
        Optional<String> zip,
        Optional<ShippingCountry> country,
        Optional<String> phoneNumber,
        Optional<String> email,
        Optional<PickupType> pickupType,
        Optional<String> pointCode) {

    public ShippingParty {
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(companyName, "companyName");
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(buildingNumber, "buildingNumber");
        Objects.requireNonNull(flatNumber, "flatNumber");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(zip, "zip");
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(phoneNumber, "phoneNumber");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(pickupType, "pickupType");
        Objects.requireNonNull(pointCode, "pointCode");
    }

    /**
     * Renders the party without its personal data: identifying fields collapse to a redaction marker,
     * while non-identifying routing detail (country, pickup type, pickup-point code) stays readable so
     * the output remains useful for diagnosing a delivery problem.
     */
    @Override
    public String toString() {
        return "ShippingParty[firstName=" + Redaction.hide(firstName)
                + ", lastName=" + Redaction.hide(lastName)
                + ", companyName=" + Redaction.hide(companyName)
                + ", street=" + Redaction.hide(street)
                + ", buildingNumber=" + Redaction.hide(buildingNumber)
                + ", flatNumber=" + Redaction.hide(flatNumber)
                + ", city=" + Redaction.hide(city)
                + ", zip=" + Redaction.hide(zip)
                + ", country=" + country.map(Enum::name).orElse(null)
                + ", phoneNumber=" + Redaction.hide(phoneNumber)
                + ", email=" + Redaction.hide(email)
                + ", pickupType=" + pickupType.map(Enum::name).orElse(null)
                + ", pointCode=" + pointCode.orElse(null)
                + ']';
    }
}
