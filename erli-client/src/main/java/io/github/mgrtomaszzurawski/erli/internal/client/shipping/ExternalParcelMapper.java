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
import io.github.mgrtomaszzurawski.erli.rest.model.CreateExternalParcelResponseAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ErrorResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ExternalParcelShipping;
import io.github.mgrtomaszzurawski.erli.rest.model.ParcelStatusHistoryInner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
                List.of(),
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
     * <p><strong>CORE-3 workaround — remove when bucket B's PR lands.</strong> The generated
     * {@code anyOf} deserializer takes the first branch that parses, and because the shared codec
     * ignores unknown properties the "created" branch always parses — a refused entry would bind to it
     * and arrive without its {@code error} list, which is the only thing that entry is for. Until the
     * {@code normalizeSpec} composite-merge makes Layer 1 lossless, this dispatches on the raw tree
     * (the entry carries {@code error} only when refused) and binds each shape explicitly. At that
     * point this method collapses back to a single {@code getActualInstance()} dispatch.
     */
    static ExternalParcelResult toResult(JsonNode rawEntry) {
        Objects.requireNonNull(rawEntry, "raw external parcel result");
        if (rawEntry.has(ERROR_FIELD)) {
            return toRejected(rawEntry);
        }
        return new ExternalParcelResult.Created(toCreatedFromTree(rawEntry));
    }

    private static ExternalParcel toCreatedFromTree(JsonNode rawEntry) {
        CreateExternalParcelResponseAnyOf created = new CreateExternalParcelResponseAnyOf();
        created.setId(intOrNull(rawEntry, "id"));
        created.setOrderId(textOrNull(rawEntry, "orderId"));
        created.setType(CreateExternalParcelResponseAnyOf.TypeEnum.fromValue(
                requireField(textOrNull(rawEntry, "type"), "type")));
        ExternalParcelShipping shipping = new ExternalParcelShipping();
        JsonNode rawShipping = rawEntry.get("shipping");
        shipping.setVendor(ExternalParcelShipping.VendorEnum.fromValue(
                requireField(rawShipping == null ? null : textOrNull(rawShipping, "vendor"), "shipping.vendor")));
        created.setShipping(shipping);
        created.setStatus(CreateExternalParcelResponseAnyOf.StatusEnum.fromValue(
                requireField(textOrNull(rawEntry, "status"), "status")));
        created.setTrackingNumber(textOrNull(rawEntry, "trackingNumber"));
        created.setTrackingStoppedCause(textOrNull(rawEntry, "trackingStoppedCause"));
        created.setCreatedAt(timestamp(rawEntry, "createdAt"));
        created.setUpdatedAt(timestamp(rawEntry, "updatedAt"));
        return toCreated(created);
    }

    private static ExternalParcelResult toRejected(JsonNode rawEntry) {
        List<ParcelError> errors = new ArrayList<>();
        for (JsonNode rawError : rawEntry.get(ERROR_FIELD)) {
            JsonNode code = rawError.get("errorCode");
            if (code == null) {
                throw new IllegalStateException("External parcel 'error[].errorCode' is missing");
            }
            errors.add(new ParcelError(code.intValue(),
                    Optional.ofNullable(textOrNull(rawError, "errorMessage"))));
        }
        return new ExternalParcelResult.Rejected(
                OrderId.of(requireField(textOrNull(rawEntry, "orderId"), "orderId")),
                Optional.ofNullable(textOrNull(rawEntry, "vendor")).map(ShippingVendor::fromWire),
                Optional.ofNullable(textOrNull(rawEntry, "trackingNumber")),
                errors);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.intValue();
    }

    private static OffsetDateTime timestamp(JsonNode node, String field) {
        String text = textOrNull(node, field);
        return text == null ? null : OffsetDateTime.parse(text);
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
