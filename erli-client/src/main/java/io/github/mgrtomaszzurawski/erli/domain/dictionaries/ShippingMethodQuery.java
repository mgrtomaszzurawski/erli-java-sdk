package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;

/**
 * Optional server-side filters for {@link DictionariesAccess#shippingMethods(ShippingMethodQuery)}.
 * A {@code null} component means "do not filter on this"; use {@link #none()} for the whole
 * dictionary.
 *
 * @param id keep only the method with this identifier
 * @param groupId keep only methods in this delivery group
 * @param operator keep only methods run by this operator
 * @param cashOnDelivery keep only methods with (or without) cash-on-delivery support
 */
public record ShippingMethodQuery(
        ShippingMethodId id,
        String groupId,
        ShippingOperator operator,
        Boolean cashOnDelivery) {

    private static final ShippingMethodQuery NONE = new ShippingMethodQuery(null, null, null, null);

    /** The empty query — every shipping method. */
    public static ShippingMethodQuery none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link ShippingMethodQuery}. */
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

        public Builder cashOnDelivery(Boolean value) {
            this.cashOnDelivery = value;
            return this;
        }

        public ShippingMethodQuery build() {
            return new ShippingMethodQuery(id, groupId, operator, cashOnDelivery);
        }
    }
}
