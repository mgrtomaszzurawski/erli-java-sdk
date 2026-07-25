package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;

import java.util.Objects;
import java.util.Optional;

/**
 * Optional server-side filters for the ERLI shipping-method dictionary.
 *
 * @param id keep only the method with this identifier
 * @param groupId keep only methods in this delivery group
 * @param operator keep only methods run by this operator
 * @param cashOnDelivery keep only methods with (or without) cash-on-delivery support
 */
public record ShippingMethodFilter(
        Optional<ShippingMethodId> id,
        Optional<String> groupId,
        Optional<ShippingOperator> operator,
        Optional<Boolean> cashOnDelivery) {

    private static final ShippingMethodFilter ALL = new ShippingMethodFilter(
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    public ShippingMethodFilter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(cashOnDelivery, "cashOnDelivery");
    }

    /** The empty filter — every shipping method. */
    public static ShippingMethodFilter all() {
        return ALL;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link ShippingMethodFilter}. */
    public static final class Builder {

        private ShippingMethodId id;
        private String groupId;
        private ShippingOperator operator;
        private Boolean cashOnDelivery;

        private Builder() {
        }

        public Builder id(ShippingMethodId value) {
            this.id = value;
            return this;
        }

        public Builder groupId(String value) {
            this.groupId = value;
            return this;
        }

        public Builder operator(ShippingOperator value) {
            this.operator = value;
            return this;
        }

        public Builder cashOnDelivery(boolean value) {
            this.cashOnDelivery = value;
            return this;
        }

        public ShippingMethodFilter build() {
            return new ShippingMethodFilter(
                    Optional.ofNullable(id),
                    Optional.ofNullable(groupId),
                    Optional.ofNullable(operator),
                    Optional.ofNullable(cashOnDelivery));
        }
    }
}
