package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of registering one external parcel in a batch.
 *
 * <p>{@code POST /shipping/external} takes a list and answers with a list of the same length: Erli
 * accepts each entry independently, so one bad entry does not fail the others. The API expresses that
 * as two payload shapes — the created parcel, or the rejected entry with its errors — which the SDK
 * keeps apart as a sealed pair so a caller cannot read a parcel that was never created.
 */
public sealed interface ExternalParcelResult
        permits ExternalParcelResult.Created, ExternalParcelResult.Rejected {

    /** The order the entry referred to, whether or not it was accepted. */
    OrderId orderId();

    /** The registered parcel. */
    record Created(ExternalParcel parcel) implements ExternalParcelResult {

        public Created {
            Objects.requireNonNull(parcel, "parcel");
        }

        @Override
        public OrderId orderId() {
            return parcel.orderId();
        }
    }

    /**
     * The entry Erli refused, echoed back with the reasons.
     *
     * @param orderId        the order the entry referred to
     * @param vendor         the carrier the entry named, when it named a known one
     * @param trackingNumber the tracking number the entry carried, when it had one
     * @param errors         why the entry was refused; never empty
     */
    record Rejected(
            OrderId orderId,
            Optional<ShippingVendor> vendor,
            Optional<String> trackingNumber,
            List<ParcelError> errors) implements ExternalParcelResult {

        public Rejected {
            Objects.requireNonNull(orderId, "orderId");
            Objects.requireNonNull(vendor, "vendor");
            Objects.requireNonNull(trackingNumber, "trackingNumber");
            errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        }
    }
}
