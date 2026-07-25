package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A place the seller hands parcels over to a carrier, as configured in the shop panel.
 *
 * <p>The API models the three kinds as separate payload shapes sharing one base — a courier collecting
 * from an address, a single drop-off point, or a choice of several. The SDK exposes one record and
 * discriminates on {@link #type()}: {@code points} is populated only for {@link PostingPointType#POINTS},
 * and {@code point} only for {@link PostingPointType#POINT}.
 *
 * <p>The contact details here belong to the seller's own site, not to a buyer, so they are not
 * redacted.
 *
 * @param id             the posting point's numeric id
 * @param name           its display name
 * @param type           how parcels are handed over
 * @param isDefault      whether it is the shop's default posting point
 * @param companyName    the company at this location
 * @param phone          contact phone number
 * @param email          contact e-mail
 * @param street         street name
 * @param buildingNumber building number
 * @param flatNumber     flat number, when there is one
 * @param zip            postcode
 * @param city           city
 * @param point          the single carrier point used, for {@link PostingPointType#POINT}
 * @param points         the carrier points available, for {@link PostingPointType#POINTS}
 */
public record PostingPoint(
        long id,
        String name,
        PostingPointType type,
        boolean isDefault,
        Optional<String> companyName,
        Optional<String> phone,
        Optional<String> email,
        Optional<String> street,
        Optional<String> buildingNumber,
        Optional<String> flatNumber,
        Optional<String> zip,
        Optional<String> city,
        Optional<CarrierPoint> point,
        List<CarrierPoint> points) {

    public PostingPoint {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(companyName, "companyName");
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(buildingNumber, "buildingNumber");
        Objects.requireNonNull(flatNumber, "flatNumber");
        Objects.requireNonNull(zip, "zip");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(point, "point");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
    }
}
