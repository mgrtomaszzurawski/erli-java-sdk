package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelResult;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelError;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatus;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatusChange;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelType;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingVendor;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateExternalParcelResponseAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateExternalParcelResponseAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.ErrorResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ExternalParcelShipping;
import io.github.mgrtomaszzurawski.erli.rest.model.ParcelStatusHistoryInner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated external-parcel models onto the public domain records. Internal: never exported.
 */
final class ExternalParcelMapper {

    /** Present only on a refused entry — the discriminator the anyOf branches do not give us. */
    private static final String ERROR_FIELD = "error";

    private ExternalParcelMapper() {
    }

    static ExternalParcel toDomain(io.github.mgrtomaszzurawski.erli.rest.model.ExternalParcel rawParcel) {
        Objects.requireNonNull(rawParcel, "raw ExternalParcel");
        return new ExternalParcel(
                ParcelId.of(String.valueOf(requireField(rawParcel.getId(), "id"))),
                OrderId.of(requireField(rawParcel.getOrderId(), "orderId")),
                ParcelType.fromWire(requireField(rawParcel.getType(), "type").getValue()),
                toVendor(requireField(rawParcel.getShipping(), "shipping")),
                ParcelStatus.fromWire(requireField(rawParcel.getStatus(), "status").getValue()),
                toStatusHistory(rawParcel.getStatusHistory()),
                Optional.ofNullable(rawParcel.getTrackingNumber()),
                Optional.ofNullable(rawParcel.getTrackingStoppedCause()),
                requireField(rawParcel.getCreatedAt(), "createdAt"),
                requireField(rawParcel.getUpdatedAt(), "updatedAt"));
    }

    /**
     * Map one entry of a batch-create response.
     *
     * <p>The API answers per entry with either the created parcel or the refused entry echoed back with
     * its errors, stated as an {@code anyOf} over the two shapes.
     *
     * <p><strong>CORE-3 workaround — delete the tree dispatch when bucket B's PR lands.</strong> The
     * generated {@code anyOf} deserializer takes the first branch that parses, and because the shared
     * codec ignores unknown properties the "created" branch always parses — a refused entry would bind
     * to it and arrive without its {@code error} list, which is the only thing that entry carries.
     * Until the {@code normalizeSpec} composite-merge makes Layer 1 lossless, the discriminator is read
     * off the raw tree ({@code error} is present only on a refusal) and the node is then bound to the
     * right generated type by the shared codec — so every field still comes from Layer 1, not from
     * hand-copied field names. Afterwards this collapses to a single {@code getActualInstance()} switch.
     */
    static ExternalParcelResult toResult(JsonNode rawEntry, JsonCodec codec) {
        Objects.requireNonNull(rawEntry, "raw external parcel result");
        Objects.requireNonNull(codec, "codec");
        if (rawEntry.has(ERROR_FIELD)) {
            return toRejected(codec.convert(rawEntry, CreateExternalParcelResponseAnyOf1.class));
        }
        return new ExternalParcelResult.Created(
                toCreated(codec.convert(rawEntry, CreateExternalParcelResponseAnyOf.class)));
    }

    private static ExternalParcelResult toRejected(CreateExternalParcelResponseAnyOf1 raw) {
        return new ExternalParcelResult.Rejected(
                OrderId.of(requireField(raw.getOrderId(), "orderId")),
                Optional.ofNullable(raw.getVendor())
                        .map(CreateExternalParcelResponseAnyOf1.VendorEnum::getValue)
                        .map(ShippingVendor::fromWire),
                Optional.ofNullable(raw.getTrackingNumber())
                        .map(trackingNumber -> String.valueOf(trackingNumber.getActualInstance())),
                toErrors(raw.getError()));
    }

    private static ExternalParcel toCreated(CreateExternalParcelResponseAnyOf raw) {
        return new ExternalParcel(
                ParcelId.of(String.valueOf(requireField(raw.getId(), "id"))),
                OrderId.of(requireField(raw.getOrderId(), "orderId")),
                ParcelType.fromWire(requireField(raw.getType(), "type").getValue()),
                toVendor(requireField(raw.getShipping(), "shipping")),
                ParcelStatus.fromWire(requireField(raw.getStatus(), "status").getValue()),
                toStatusHistory(raw.getStatusHistory()),
                Optional.ofNullable(raw.getTrackingNumber()),
                Optional.ofNullable(raw.getTrackingStoppedCause()),
                requireField(raw.getCreatedAt(), "createdAt"),
                requireField(raw.getUpdatedAt(), "updatedAt"));
    }

    private static ShippingVendor toVendor(ExternalParcelShipping rawShipping) {
        return ShippingVendor.fromWire(requireField(rawShipping.getVendor(), "shipping.vendor").getValue());
    }

    private static List<ParcelStatusChange> toStatusHistory(List<ParcelStatusHistoryInner> rawHistory) {
        if (rawHistory == null) {
            return List.of();
        }
        List<ParcelStatusChange> history = new ArrayList<>(rawHistory.size());
        for (ParcelStatusHistoryInner rawEntry : rawHistory) {
            if (rawEntry == null) {
                throw new IllegalStateException("External parcel 'statusHistory' has a null element");
            }
            history.add(new ParcelStatusChange(
                    ParcelStatus.fromWire(requireField(rawEntry.getStatus(), "statusHistory[].status").getValue()),
                    Optional.ofNullable(rawEntry.getChanged())));
        }
        return List.copyOf(history);
    }

    private static List<ParcelError> toErrors(List<ErrorResponseInner> rawErrors) {
        if (rawErrors == null) {
            return List.of();
        }
        List<ParcelError> errors = new ArrayList<>(rawErrors.size());
        for (ErrorResponseInner rawError : rawErrors) {
            if (rawError == null) {
                throw new IllegalStateException("External parcel 'error' has a null element");
            }
            BigDecimal errorCode = requireField(rawError.getErrorCode(), "error[].errorCode");
            errors.add(new ParcelError(errorCode.intValue(), Optional.ofNullable(rawError.getErrorMessage())));
        }
        return List.copyOf(errors);
    }

    private static <T> T requireField(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(
                    "External parcel is missing the required '" + fieldName + "' field");
        }
        return value;
    }
}
