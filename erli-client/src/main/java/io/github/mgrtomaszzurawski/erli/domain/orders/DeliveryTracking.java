package io.github.mgrtomaszzurawski.erli.domain.orders;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;

import java.util.Optional;

/**
 * Tracking information for the order's parcel.
 *
 * <p>Erli models this as a choice between two shapes — a bare tracking URL, or a carrier plus a
 * consignment number. Both collapse into this one record because they differ only in which optional
 * fields are populated; {@link #status()} is the one field always present. A record with everything
 * optional-but-status is simpler for a consumer than a sealed pair they must switch over.
 *
 * @param status         where the parcel is
 * @param vendor         the carrier, when Erli reports a tracked consignment
 * @param trackingNumber the consignment number, when Erli reports a tracked consignment
 * @param trackingUrl    a carrier tracking page, when Erli reports a bare URL instead
 */
public record DeliveryTracking(
        TrackingStatus status,
        Optional<DeliveryVendor> vendor,
        Optional<String> trackingNumber,
        Optional<String> trackingUrl) {
}
