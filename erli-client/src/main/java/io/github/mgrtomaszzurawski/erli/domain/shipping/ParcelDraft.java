package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;

import java.util.Objects;
import java.util.Optional;

/**
 * A parcel to hand to Erli's carrier integration. Build one with
 * {@link #builder(OrderId, ShippingMethodId, ParcelDimensions, ShippingParty)}.
 *
 * <p>Two API rules worth knowing before calling: a parcel cannot be added to a cancelled order, and
 * when a second parcel is added to an order the receiver address must match the first one exactly —
 * street, building number, flat number, city, postcode and country. A mismatch is answered with a
 * validation error naming the differing fields.
 *
 * @param orderId               the order being shipped
 * @param deliveryMethod        the Erli delivery method to use ({@code typeId} on the wire)
 * @param dimensions            size and weight, in millimetres and grams
 * @param receiver              where the parcel goes — carries buyer personal data
 * @param postingPointId        the posting point to hand the parcel in at, when not the default
 * @param additionalInformation free-text note for the courier
 * @param nonStandard           declare the parcel non-standard sized; only valid for courier delivery
 */
public record ParcelDraft(
        OrderId orderId,
        ShippingMethodId deliveryMethod,
        ParcelDimensions dimensions,
        ShippingParty receiver,
        Optional<Long> postingPointId,
        Optional<String> additionalInformation,
        boolean nonStandard) {

    public ParcelDraft {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(deliveryMethod, "deliveryMethod");
        Objects.requireNonNull(dimensions, "dimensions");
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(postingPointId, "postingPointId");
        Objects.requireNonNull(additionalInformation, "additionalInformation");
    }

    /**
     * Renders the draft without its personal data. {@link ShippingParty} redacts itself, but
     * {@code additionalInformation} is free text for the courier ("leave with the neighbour at flat 3,
     * gate code 1234") and routinely restates the delivery address — the read side hides the same field
     * on {@link ParcelShipment}, and an outbound draft must not leak what the inbound record protects.
     */
    @Override
    public String toString() {
        return "ParcelDraft[orderId=" + orderId
                + ", deliveryMethod=" + deliveryMethod
                + ", dimensions=" + dimensions
                + ", receiver=" + receiver
                + ", postingPointId=" + postingPointId.map(String::valueOf).orElse("null")
                + ", additionalInformation=" + (additionalInformation.isEmpty() ? "null" : "***")
                + ", nonStandard=" + nonStandard
                + ']';
    }

    public static Builder builder(
            OrderId orderId, ShippingMethodId deliveryMethod, ParcelDimensions dimensions,
            ShippingParty receiver) {
        return new Builder(orderId, deliveryMethod, dimensions, receiver);
    }

    /** Builder for {@link ParcelDraft}. */
    public static final class Builder {

        private final OrderId orderId;
        private final ShippingMethodId deliveryMethod;
        private final ParcelDimensions dimensions;
        private final ShippingParty receiver;
        private Long postingPointId;
        private String additionalInformation;
        private boolean nonStandard;

        private Builder(OrderId orderId, ShippingMethodId deliveryMethod, ParcelDimensions dimensions,
                ShippingParty receiver) {
            this.orderId = Objects.requireNonNull(orderId, "orderId");
            this.deliveryMethod = Objects.requireNonNull(deliveryMethod, "deliveryMethod");
            this.dimensions = Objects.requireNonNull(dimensions, "dimensions");
            this.receiver = Objects.requireNonNull(receiver, "receiver");
        }

        public Builder postingPointId(long value) {
            this.postingPointId = value;
            return this;
        }

        public Builder additionalInformation(String value) {
            this.additionalInformation = value;
            return this;
        }

        public Builder nonStandard(boolean value) {
            this.nonStandard = value;
            return this;
        }

        public ParcelDraft build() {
            return new ParcelDraft(orderId, deliveryMethod, dimensions, receiver,
                    Optional.ofNullable(postingPointId), Optional.ofNullable(additionalInformation), nonStandard);
        }
    }
}
