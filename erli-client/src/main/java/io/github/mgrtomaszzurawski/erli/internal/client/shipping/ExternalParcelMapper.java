package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelResult;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelError;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatus;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatusChange;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelType;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateExternalParcelInnerTrackingNumber;
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

    private static final String FIELD_ORDER_ID = "orderId";

    /** Present only on a refused entry — the discriminator the anyOf branches do not give us. */
    private static final String ERROR_FIELD = "error";

    private ExternalParcelMapper() {
    }

    static ExternalParcel toDomain(io.github.mgrtomaszzurawski.erli.rest.model.ExternalParcel rawParcel) {
        Objects.requireNonNull(rawParcel, "raw ExternalParcel");
        return new ExternalParcel(
                ParcelId.of(String.valueOf(requireField(rawParcel.getId(), "id"))),
                OrderId.of(requireField(rawParcel.getOrderId(), FIELD_ORDER_ID)),
                ParcelType.fromWire(requireField(rawParcel.getType(), "type").getValue()),
                toVendor(requireField(rawParcel.getShipping(), "shipping")),
                toStatus(rawParcel.getStatus()),
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
     * <p><strong>Why the discriminator is read off the raw tree — do not "simplify" this away.</strong>
     * The generated {@code anyOf} deserializer is first-match-wins, and the shared codec both ignores
     * unknown properties and decodes an unknown enum value as {@code null}, so the "created" branch
     * parses a refusal too and drops the {@code error} list that is the only thing that entry carries.
     *
     * <p>The {@code normalizeSpec} composite-merge (CORE-3) does <em>not</em> cover this schema and is
     * not going to: it merges only composites whose branches agree on every shared property, and these
     * two disagree on {@code orderId}, {@code status} and {@code trackingNumber}. That guard is correct
     * — merging a discriminated union would destroy the discriminator. Verified against the vendored
     * spec on 2026-07-25, when the merge reported the five {@code deliveryTracking} composites and
     * nothing else.
     *
     * <p>The node is bound to the matching generated class by the shared codec, so every field still
     * comes from Layer 1 rather than from hand-copied names.
     */
    static ExternalParcelResult toResult(JsonNode rawEntry, JsonCodec codec) {
        Objects.requireNonNull(rawEntry, "raw external parcel result");
        Objects.requireNonNull(codec, "codec");
        // hasNonNull, not has: an explicit "error": null is a created parcel, not a refusal.
        if (rawEntry.hasNonNull(ERROR_FIELD)) {
            return toRejected(codec.convert(rawEntry, CreateExternalParcelResponseAnyOf1.class));
        }
        return new ExternalParcelResult.Created(
                toCreated(codec.convert(rawEntry, CreateExternalParcelResponseAnyOf.class)));
    }

    private static ExternalParcelResult toRejected(CreateExternalParcelResponseAnyOf1 raw) {
        return new ExternalParcelResult.Rejected(
                OrderId.of(requireField(raw.getOrderId(), FIELD_ORDER_ID)),
                Optional.ofNullable(raw.getVendor())
                        .map(CreateExternalParcelResponseAnyOf1.VendorEnum::getValue)
                        .map(DeliveryVendor::fromWire),
                Optional.ofNullable(raw.getTrackingNumber())
                        .map(CreateExternalParcelInnerTrackingNumber::getString),
                toErrors(raw.getError()));
    }

    private static ExternalParcel toCreated(CreateExternalParcelResponseAnyOf raw) {
        return new ExternalParcel(
                ParcelId.of(String.valueOf(requireField(raw.getId(), "id"))),
                OrderId.of(requireField(raw.getOrderId(), FIELD_ORDER_ID)),
                ParcelType.fromWire(requireField(raw.getType(), "type").getValue()),
                toVendor(requireField(raw.getShipping(), "shipping")),
                toStatus(raw.getStatus()),
                toStatusHistory(raw.getStatusHistory()),
                Optional.ofNullable(raw.getTrackingNumber()),
                Optional.ofNullable(raw.getTrackingStoppedCause()),
                requireField(raw.getCreatedAt(), "createdAt"),
                requireField(raw.getUpdatedAt(), "updatedAt"));
    }

    private static DeliveryVendor toVendor(ExternalParcelShipping rawShipping) {
        return DeliveryVendor.fromWire(requireField(rawShipping.getVendor(), "shipping.vendor").getValue());
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
                    toStatus(rawEntry.getStatus()),
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

    /**
     * Tolerant like {@link ParcelMapper}: an unknown or absent status degrades to a sentinel.
     *
     * <p>Three generated enum types carry a parcel status, with no common supertype, so there is one
     * overload each rather than an {@code Object} parameter — {@code getValue()} is the {@code @JsonValue}
     * contract, whereas {@code toString()} agreeing with it today is a generator artifact, and an
     * {@code Object} parameter would switch the compiler off for all three.
     */
    private static ParcelStatus toStatus(io.github.mgrtomaszzurawski.erli.rest.
            model.ExternalParcel.StatusEnum rawStatus) {
        return rawStatus == null ? ParcelStatus.UNRECOGNIZED : ParcelStatus.fromWire(rawStatus.getValue());
    }

    private static ParcelStatus toStatus(CreateExternalParcelResponseAnyOf.StatusEnum rawStatus) {
        return rawStatus == null ? ParcelStatus.UNRECOGNIZED : ParcelStatus.fromWire(rawStatus.getValue());
    }

    private static ParcelStatus toStatus(ParcelStatusHistoryInner.StatusEnum rawStatus) {
        return rawStatus == null ? ParcelStatus.UNRECOGNIZED : ParcelStatus.fromWire(rawStatus.getValue());
    }

    private static <T> T requireField(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(
                    "External parcel is missing the required '" + fieldName + "' field");
        }
        return value;
    }
}
