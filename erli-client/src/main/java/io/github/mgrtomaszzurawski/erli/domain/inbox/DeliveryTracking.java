package io.github.mgrtomaszzurawski.erli.domain.inbox;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;

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
 * <p>{@link #vendor()} is the carrier enum owned by {@code domain.dictionaries} — the fan-out plan's
 * reference hub. Duplicating those 26 constants into this package would put two mutually incompatible
 * carrier types on the SDK's exported surface, which is worse for consumers than the one cross-package
 * reference; promoting it to {@code core.model} is filed as a backlog item.
 *
 * @param status         where the parcel is
 * @param trackingUrl    a page the buyer can follow the parcel on
 * @param vendor         the carrier, when the API identifies one. Reuses the dictionaries bucket's
 *                       {@link DeliveryVendor} rather than minting a second copy of the same 26-value
 *                       carrier list — see the note in the class javadoc
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
