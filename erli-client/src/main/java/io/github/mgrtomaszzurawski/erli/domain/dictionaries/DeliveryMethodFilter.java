package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;

import java.util.Objects;
import java.util.Optional;

/**
 * Optional server-side filters for the delivery-method dictionary. Every field is optional; the
 * unfiltered {@link #all()} returns the whole dictionary.
 *
 * @param id keep only the method with this identifier
 * @param cashOnDelivery keep only methods with (or without) cash-on-delivery support
 * @param vendor keep only methods operated by this carrier
 */
public record DeliveryMethodFilter(
        Optional<DeliveryMethodId> id,
        Optional<Boolean> cashOnDelivery,
        Optional<DeliveryVendor> vendor) {

    private static final DeliveryMethodFilter ALL =
            new DeliveryMethodFilter(Optional.empty(), Optional.empty(), Optional.empty());

    public DeliveryMethodFilter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(cashOnDelivery, "cashOnDelivery");
        Objects.requireNonNull(vendor, "vendor");
    }

    /** The empty filter — every delivery method. */
    public static DeliveryMethodFilter all() {
        return ALL;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link DeliveryMethodFilter}. */
    public static final class Builder {

        private DeliveryMethodId id;
        private Boolean cashOnDelivery;
        private DeliveryVendor vendor;

        private Builder() {
        }

        public Builder id(DeliveryMethodId value) {
            this.id = value;
            return this;
        }

        public Builder cashOnDelivery(boolean value) {
            this.cashOnDelivery = value;
            return this;
        }

        public Builder vendor(DeliveryVendor value) {
            this.vendor = value;
            return this;
        }

        public DeliveryMethodFilter build() {
            return new DeliveryMethodFilter(
                    Optional.ofNullable(id),
                    Optional.ofNullable(cashOnDelivery),
                    Optional.ofNullable(vendor));
        }
    }
}
