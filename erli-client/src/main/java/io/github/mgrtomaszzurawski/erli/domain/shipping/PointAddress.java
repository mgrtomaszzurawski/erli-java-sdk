package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.util.Objects;
import java.util.Optional;

/**
 * The street address of a carrier pickup point. Unlike {@link ShippingParty} this is a public
 * business location, not personal data, so it renders in full.
 *
 * @param street         street name
 * @param buildingNumber building number
 * @param flatNumber     flat number, when the point has one
 * @param zip            postcode
 * @param city           city
 */
public record PointAddress(
        Optional<String> street,
        Optional<String> buildingNumber,
        Optional<String> flatNumber,
        Optional<String> zip,
        Optional<String> city) {

    public PointAddress {
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(buildingNumber, "buildingNumber");
        Objects.requireNonNull(flatNumber, "flatNumber");
        Objects.requireNonNull(zip, "zip");
        Objects.requireNonNull(city, "city");
    }
}
