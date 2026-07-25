package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;

import java.util.Objects;
import java.util.Optional;

/**
 * An externally shipped parcel to register. Build one with {@link #builder(OrderId, DeliveryVendor)}.
 *
 * <p>{@code status} is <em>required</em> — not merely permitted — for the sixteen carriers Erli cannot
 * track itself (own transport, self pickup and the pallet forwarders); the spec says
 * {@code wymagane gdy}, and distinguishes that from {@code dopuszczalne gdy} elsewhere on the same
 * schema. For the tracked carriers Erli derives it, so leave it unset. The SDK does not enforce the
 * pairing; omitting a required status is refused server-side. {@code deliveryMethod} narrows a vendor
 * that runs several services, e.g. DPD's domestic and German networks.
 *
 * @param orderId        the order being shipped
 * @param vendor         the carrier the seller used
 * @param status         the status to set; required for the vendors Erli cannot track itself
 * @param trackingNumber the carrier tracking number, when there is one
 * @param deliveryMethod the vendor's specific service, when it has more than one
 */
public record ExternalParcelDraft(
        OrderId orderId,
        DeliveryVendor vendor,
        Optional<ParcelStatus> status,
        Optional<String> trackingNumber,
        Optional<ShippingMethodId> deliveryMethod) {

    public ExternalParcelDraft {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(vendor, "vendor");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        Objects.requireNonNull(deliveryMethod, "deliveryMethod");
    }

    public static Builder builder(OrderId orderId, DeliveryVendor vendor) {
        return new Builder(orderId, vendor);
    }

    /** Builder for {@link ExternalParcelDraft}. */
    public static final class Builder {

        private final OrderId orderId;
        private final DeliveryVendor vendor;
        private ParcelStatus status;
        private String trackingNumber;
        private ShippingMethodId deliveryMethod;

        private Builder(OrderId orderId, DeliveryVendor vendor) {
            this.orderId = Objects.requireNonNull(orderId, "orderId");
            this.vendor = Objects.requireNonNull(vendor, "vendor");
        }

        public Builder status(ParcelStatus value) {
            this.status = value;
            return this;
        }

        public Builder trackingNumber(String value) {
            this.trackingNumber = value;
            return this;
        }

        public Builder deliveryMethod(ShippingMethodId value) {
            this.deliveryMethod = value;
            return this;
        }

        public ExternalParcelDraft build() {
            return new ExternalParcelDraft(orderId, vendor, Optional.ofNullable(status),
                    Optional.ofNullable(trackingNumber), Optional.ofNullable(deliveryMethod));
        }
    }
}
