package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import java.util.Objects;
import java.util.Optional;

/**
 * Changes to apply to a registered external parcel. Build one with {@link #builder(DeliveryVendor)}.
 *
 * <p>The vendor is required on every update — the API treats the patch body as the parcel's new
 * carriage description, not as a partial diff. A parcel that has already been dispatched or cancelled
 * cannot be edited; the API answers those with a validation error.
 *
 * @param vendor         the carrier, restated on every update
 * @param status         the status to set; required for the vendors Erli cannot track itself
 * @param trackingNumber the carrier tracking number, when there is one
 * @param deliveryMethod the vendor's specific service, when it has more than one
 */
public record ExternalParcelUpdate(
        DeliveryVendor vendor,
        Optional<ParcelStatus> status,
        Optional<String> trackingNumber,
        Optional<ShippingMethodId> deliveryMethod) {

    public ExternalParcelUpdate {
        Objects.requireNonNull(vendor, "vendor");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(trackingNumber, "trackingNumber");
        Objects.requireNonNull(deliveryMethod, "deliveryMethod");
    }

    public static Builder builder(DeliveryVendor vendor) {
        return new Builder(vendor);
    }

    /** Builder for {@link ExternalParcelUpdate}. */
    public static final class Builder {

        private final DeliveryVendor vendor;
        private ParcelStatus status;
        private String trackingNumber;
        private ShippingMethodId deliveryMethod;

        private Builder(DeliveryVendor vendor) {
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

        public ExternalParcelUpdate build() {
            return new ExternalParcelUpdate(vendor, Optional.ofNullable(status),
                    Optional.ofNullable(trackingNumber), Optional.ofNullable(deliveryMethod));
        }
    }
}
