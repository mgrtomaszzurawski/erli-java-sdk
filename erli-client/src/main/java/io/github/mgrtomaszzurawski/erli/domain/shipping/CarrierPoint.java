package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import java.util.Objects;
import java.util.Optional;

/**
 * One carrier pickup point a posting point hands parcels in at — a locker or a service point.
 *
 * @param pointCode      the carrier's code for the point
 * @param pointAddress   where it is, as a street address
 * @param location       where it is, as coordinates
 * @param deliveryMethod the delivery method this point serves, when the API states one
 */
public record CarrierPoint(
        Optional<String> pointCode,
        Optional<PointAddress> pointAddress,
        Optional<GeoLocation> location,
        Optional<ShippingMethodId> deliveryMethod) {

    public CarrierPoint {
        Objects.requireNonNull(pointCode, "pointCode");
        Objects.requireNonNull(pointAddress, "pointAddress");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(deliveryMethod, "deliveryMethod");
    }
}
