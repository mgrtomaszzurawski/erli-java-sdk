package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.Objects;
import java.util.Optional;

/**
 * Tracking information for the order's parcel.
 *
 * <p>The API declares two alternative shapes for this object: one carrying a tracking <em>URL</em>,
 * one carrying a <em>carrier plus tracking number</em>. Both always carry the status, so the SDK
 * flattens them into a single record with optional components rather than making callers switch over
 * two near-identical types.
 *
 * @param status         where the parcel is
 * @param trackingUrl    a page the buyer can follow the parcel on
 * @param vendor         the carrier, when the API identifies one
 * @param trackingNumber the carrier's tracking number, when the API reports one
 */
public record DeliveryTracking(
        TrackingStatus status,
        Optional<String> trackingUrl,
        Optional<DeliveryVendor> vendor,
        Optional<String> trackingNumber) {

    public DeliveryTracking {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(trackingUrl, "trackingUrl");
        Objects.requireNonNull(vendor, "vendor");
        Objects.requireNonNull(trackingNumber, "trackingNumber");
    }
}
