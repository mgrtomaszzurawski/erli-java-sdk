package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

/**
 * Optional filters for {@link DictionariesAccess#deliveryMethods(DeliveryMethodQuery)}. Every field is
 * nullable; a null field is simply omitted from the request. This is a plain, exported value object —
 * it exposes no transport type; the bucket's internal client turns it into query parameters.
 *
 * @param id             filter by delivery-method id, or {@code null}
 * @param cashOnDelivery filter by COD support, or {@code null}
 * @param vendor         filter by carrier, or {@code null}
 */
public record DeliveryMethodQuery(String id, Boolean cashOnDelivery, DeliveryVendor vendor) {

    private static final DeliveryMethodQuery NONE = new DeliveryMethodQuery(null, null, null);

    /** An empty query (no filters). */
    public static DeliveryMethodQuery none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link DeliveryMethodQuery}. */
    public static final class Builder {

        private String id;
        private Boolean cashOnDelivery;
        private DeliveryVendor vendor;

        private Builder() {
        }

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder cashOnDelivery(Boolean value) {
            this.cashOnDelivery = value;
            return this;
        }

        public Builder vendor(DeliveryVendor value) {
            this.vendor = value;
            return this;
        }

        public DeliveryMethodQuery build() {
            return new DeliveryMethodQuery(id, cashOnDelivery, vendor);
        }
    }
}
