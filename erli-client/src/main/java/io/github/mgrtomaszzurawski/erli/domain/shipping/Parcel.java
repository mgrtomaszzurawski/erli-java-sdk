package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A parcel shipped through Erli's carrier integration — the unit the seller creates, tracks and can
 * cancel.
 *
 * <p>{@link #id()} is absent only while a parcel is being created: Erli assigns the id when the
 * shipment is accepted, so a parcel that came back from a read endpoint always has one. A parcel in
 * {@link ParcelStatus#ERROR} carries the reason in {@link #errors()} rather than failing the request —
 * a {@code 200} response can still describe a parcel the carrier rejected.
 *
 * <p><strong>Personal data.</strong> {@link ParcelShipment#receiver()} holds the buyer's name, address,
 * phone number and e-mail. It is redacted in {@code toString()}; see {@link ShippingParty}.
 *
 * @param id            the parcel id, once Erli has assigned one
 * @param type          who carries the shipment
 * @param orderId       the order being shipped, when the API supplied it
 * @param erliPro       whether the parcel ships free under the ErliPRO programme
 * @param dimensions    size and weight as priced by the carrier
 * @param errors        processing errors recorded on the parcel; empty when there are none
 * @param status        the current lifecycle status
 * @param statusHistory past transitions, oldest first as returned by the API; may be empty
 * @param shipping      carriage details: method, posting point, parties and documents
 * @param trackingNumber the carrier tracking number, once issued
 * @param createdAt     when the parcel was created
 * @param updatedAt     when the parcel last changed
 */
public record Parcel(
        Optional<ParcelId> id,
        ParcelType type,
        Optional<OrderId> orderId,
        boolean erliPro,
        ParcelDimensions dimensions,
        List<ParcelError> errors,
        ParcelStatus status,
        List<ParcelStatusChange> statusHistory,
        ParcelShipment shipping,
        Optional<String> trackingNumber,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public Parcel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(dimensions, "dimensions");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(shipping, "shipping");
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        statusHistory = List.copyOf(Objects.requireNonNull(statusHistory, "statusHistory"));
    }
}
