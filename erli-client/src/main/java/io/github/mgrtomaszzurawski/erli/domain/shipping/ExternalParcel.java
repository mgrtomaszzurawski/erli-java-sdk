package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A parcel the seller shipped outside Erli's carrier integration, registered here so the buyer can
 * track it.
 *
 * <p>Much thinner than {@link Parcel}: Erli never handled the package, so there are no dimensions, no
 * posting point and no waybill — only who carries it, where it is, and the tracking number.
 *
 * @param id                   the parcel id Erli assigned
 * @param orderId              the order being shipped
 * @param type                 always {@link ParcelType#EXTERNAL}
 * @param vendor               the carrier the seller used
 * @param status               the current lifecycle status
 * @param statusHistory        past transitions, as the API returned them; may be empty
 * @param trackingNumber       the carrier tracking number, when the seller supplied one
 * @param trackingStoppedCause why tracking stopped, when it did
 * @param createdAt            when the parcel was registered
 * @param updatedAt            when it last changed
 */
public record ExternalParcel(
        ParcelId id,
        OrderId orderId,
        ParcelType type,
        DeliveryVendor vendor,
        ParcelStatus status,
        List<ParcelStatusChange> statusHistory,
        Optional<String> trackingNumber,
        Optional<String> trackingStoppedCause,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public ExternalParcel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(vendor, "vendor");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        Objects.requireNonNull(trackingStoppedCause, "trackingStoppedCause");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        statusHistory = List.copyOf(Objects.requireNonNull(statusHistory, "statusHistory"));
    }
}
