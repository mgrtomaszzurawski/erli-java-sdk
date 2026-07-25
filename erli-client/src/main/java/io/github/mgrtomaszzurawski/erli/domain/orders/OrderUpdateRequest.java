package io.github.mgrtomaszzurawski.erli.domain.orders;

import java.util.Objects;
import java.util.Optional;

/**
 * The changes to apply with {@link OrderAccess#update}.
 *
 * <p>Erli's update payload also accepts a {@code deliveryTracking} block, which the API itself marks
 * deprecated in favour of {@link OrderAccess#changeStatus} for the status and {@code POST
 * /shipping/external} (the Shipping bucket) for a consignment number. It is deliberately not surfaced
 * here — see {@code KNOWN-SERVER-BEHAVIORS.md}.
 *
 * <p>An order that has been cancelled can no longer be updated; Erli answers such a call with a
 * validation error.
 */
public final class OrderUpdateRequest {

    private final Optional<String> externalOrderId;

    private OrderUpdateRequest(Builder builder) {
        this.externalOrderId = Optional.ofNullable(builder.externalOrderId);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Set the seller's own id for the order, in one call. */
    public static OrderUpdateRequest ofExternalOrderId(String externalOrderId) {
        return builder().externalOrderId(externalOrderId).build();
    }

    /** The seller's own order id to set, when this request sets one. */
    public Optional<String> externalOrderId() {
        return externalOrderId;
    }

    /** True when the request would change nothing, and so is not worth sending. */
    public boolean isEmpty() {
        return externalOrderId.isEmpty();
    }

    /** Builder for {@link OrderUpdateRequest}. */
    public static final class Builder {

        /** Erli's limit on the seller-supplied order id. */
        public static final int MAX_EXTERNAL_ORDER_ID_LENGTH = 2000;

        private String externalOrderId;

        private Builder() {
        }

        /**
         * Set the seller's own identifier for this order — typically the id it has in the seller's ERP.
         *
         * @throws IllegalArgumentException if the value is blank or longer than
         *         {@value #MAX_EXTERNAL_ORDER_ID_LENGTH} characters
         */
        public Builder externalOrderId(String value) {
            Objects.requireNonNull(value, "externalOrderId");
            if (value.isBlank()) {
                throw new IllegalArgumentException("externalOrderId must not be blank");
            }
            if (value.length() > MAX_EXTERNAL_ORDER_ID_LENGTH) {
                throw new IllegalArgumentException(
                        "externalOrderId must be at most " + MAX_EXTERNAL_ORDER_ID_LENGTH
                                + " characters, got " + value.length());
            }
            this.externalOrderId = value;
            return this;
        }

        public OrderUpdateRequest build() {
            return new OrderUpdateRequest(this);
        }
    }
}
