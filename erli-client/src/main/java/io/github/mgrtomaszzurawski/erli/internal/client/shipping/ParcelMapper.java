package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import io.github.mgrtomaszzurawski.erli.domain.shipping.Parcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelDimensions;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelError;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelShipment;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatus;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatusChange;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelType;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PickupType;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingCountry;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingParty;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateParcelsInnerDimensions;
import io.github.mgrtomaszzurawski.erli.rest.model.CreateParcelsInnerShippingReceiverZip;
import io.github.mgrtomaszzurawski.erli.rest.model.ErrorResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ParcelShipping;
import io.github.mgrtomaszzurawski.erli.rest.model.ParcelShippingReceiver;
import io.github.mgrtomaszzurawski.erli.rest.model.ParcelShippingSender;
import io.github.mgrtomaszzurawski.erli.rest.model.ParcelStatusHistoryInner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Maps the generated Layer-1 {@link io.github.mgrtomaszzurawski.erli.rest.model.Parcel} onto the public
 * {@link Parcel} domain record. Kept internal so no {@code *Raw} type reaches an exported signature.
 *
 * <p>Enums translate through each domain enum's {@code fromWire}, matching the convention the
 * Dictionaries bucket established — the schemas of this bucket repeat the status vocabulary five times,
 * so a hand-written switch per occurrence would be 98 branches. Parcel status is mapped tolerantly
 * (unknown or absent → {@link ParcelStatus#UNRECOGNIZED}); the closed vocabularies stay fail-loud.
 *
 * <p>Fields the spec marks required are asserted; a response missing one is a server contract break,
 * and failing with the field name beats handing the caller a half-built record.
 */
final class ParcelMapper {

    private ParcelMapper() {
    }

    static Parcel toDomain(io.github.mgrtomaszzurawski.erli.rest.model.Parcel rawParcel) {
        Objects.requireNonNull(rawParcel, "raw Parcel");
        return new Parcel(
                Optional.ofNullable(rawParcel.getId()).map(String::valueOf).map(ParcelId::of),
                toType(requireField(rawParcel.getType(), "type")),
                Optional.ofNullable(rawParcel.getOrderId()).map(OrderId::of),
                Boolean.TRUE.equals(rawParcel.getErliPro()),
                toDimensions(requireField(rawParcel.getDimensions(), "dimensions")),
                mapEach(rawParcel.getErrors(), ParcelMapper::toError, "errors"),
                toStatus(rawParcel.getStatus()),
                mapEach(rawParcel.getStatusHistory(), ParcelMapper::toStatusChange, "statusHistory"),
                toShipment(requireField(rawParcel.getShipping(), "shipping")),
                Optional.ofNullable(rawParcel.getTrackingNumber()),
                requireField(rawParcel.getCreatedAt(), "createdAt"),
                requireField(rawParcel.getUpdatedAt(), "updatedAt"));
    }

    private static <T> T requireField(T value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("Parcel is missing the required '" + fieldName + "' field");
        }
        return value;
    }

    /**
     * Map a nullable raw list, rejecting null elements with the same named failure the scalar fields
     * use. Without this a null element reaches {@code List.copyOf} in a record's compact constructor and
     * surfaces as a bare {@code NullPointerException} carrying no message — outside the SDK's exception
     * contract and impossible to trace back to a field.
     */
    private static <R, D> List<D> mapEach(List<R> rawItems, Function<R, D> mapper, String fieldName) {
        if (rawItems == null) {
            return List.of();
        }
        List<D> mapped = new ArrayList<>(rawItems.size());
        for (int index = 0; index < rawItems.size(); index++) {
            R rawItem = rawItems.get(index);
            if (rawItem == null) {
                throw new IllegalStateException(
                        "Parcel field '" + fieldName + "' contains a null element at index " + index);
            }
            mapped.add(mapper.apply(rawItem));
        }
        return List.copyOf(mapped);
    }

    private static ParcelType toType(io.github.mgrtomaszzurawski.erli.rest.model.Parcel.TypeEnum rawType) {
        return ParcelType.fromWire(rawType.getValue());
    }

    /**
     * Deliberately not {@code requireField}-guarded. The codec decodes an unknown enum as {@code null},
     * so a status Erli mints after this release is indistinguishable here from an absent one — and of
     * the two readings, degrading to {@link ParcelStatus#UNRECOGNIZED} is the one that does not cost the
     * caller every other parcel in the same search response. The trade is that a genuinely missing
     * status also reads as {@code UNRECOGNIZED} rather than naming a contract break.
     */
    private static ParcelStatus toStatus(
            io.github.mgrtomaszzurawski.erli.rest.model.Parcel.StatusEnum rawStatus) {
        return rawStatus == null ? ParcelStatus.UNRECOGNIZED : ParcelStatus.fromWire(rawStatus.getValue());
    }

    private static ParcelDimensions toDimensions(CreateParcelsInnerDimensions rawDimensions) {
        return new ParcelDimensions(
                requireField(rawDimensions.getWidth(), "dimensions.width"),
                requireField(rawDimensions.getHeight(), "dimensions.height"),
                requireField(rawDimensions.getLength(), "dimensions.length"),
                requireField(rawDimensions.getWeight(), "dimensions.weight"));
    }

    /**
     * The error code arrives as a JSON number, so the generated model types it {@link BigDecimal}. Erli
     * codes are whole numbers (the 1100/1200/1300/1400 families); a fractional or out-of-range one is a
     * server contract break, reported as such rather than as a bare {@code ArithmeticException}.
     */
    private static ParcelError toError(ErrorResponseInner rawError) {
        BigDecimal errorCode = requireField(rawError.getErrorCode(), "errors[].errorCode");
        try {
            return new ParcelError(errorCode.intValueExact(), Optional.ofNullable(rawError.getErrorMessage()));
        } catch (ArithmeticException notAWholeNumber) {
            throw new IllegalStateException(
                    "Parcel field 'errors[].errorCode' is not a whole 32-bit number: " + errorCode,
                    notAWholeNumber);
        }
    }

    /** Per the spec only {@code status} is required on a history entry; {@code changed} is optional. */
    private static ParcelStatusChange toStatusChange(ParcelStatusHistoryInner rawEntry) {
        ParcelStatusHistoryInner.StatusEnum rawStatus = rawEntry.getStatus();
        return new ParcelStatusChange(
                rawStatus == null ? ParcelStatus.UNRECOGNIZED : ParcelStatus.fromWire(rawStatus.getValue()),
                Optional.ofNullable(rawEntry.getChanged()));
    }

    private static ParcelShipment toShipment(ParcelShipping rawShipping) {
        return new ParcelShipment(
                Optional.ofNullable(rawShipping.getTypeId())
                        .map(ParcelShipping.TypeIdEnum::getValue)
                        .map(ShippingMethodId::of),
                Optional.ofNullable(rawShipping.getPostingPointId()).map(Integer::longValue),
                Optional.ofNullable(rawShipping.getAdditionalInformation()),
                Optional.ofNullable(rawShipping.getSender()).map(ParcelMapper::toSender),
                Optional.ofNullable(rawShipping.getReceiver()).map(ParcelMapper::toReceiver),
                Boolean.TRUE.equals(rawShipping.getNonStandard()),
                Optional.ofNullable(rawShipping.getRegisteredAt()),
                Optional.ofNullable(rawShipping.getWaybillExpiration()),
                mapEach(rawShipping.getWaybills(), Function.identity(), "shipping.waybills"),
                Optional.ofNullable(rawShipping.getPickupProtocol()));
    }

    private static ShippingParty toSender(ParcelShippingSender rawSender) {
        return new ShippingParty(
                Optional.ofNullable(rawSender.getFirstName()),
                Optional.ofNullable(rawSender.getLastName()),
                Optional.ofNullable(rawSender.getCompanyName()),
                Optional.ofNullable(rawSender.getStreet()),
                Optional.ofNullable(rawSender.getBuildingNumber()),
                Optional.ofNullable(rawSender.getFlatNumber()),
                Optional.ofNullable(rawSender.getCity()),
                toZip(rawSender.getZip(), "shipping.sender.zip"),
                Optional.ofNullable(rawSender.getCountry())
                        .map(ParcelShippingSender.CountryEnum::getValue)
                        .map(ShippingCountry::fromWire),
                Optional.ofNullable(rawSender.getPhoneNumber()),
                Optional.ofNullable(rawSender.getEmail()),
                Optional.ofNullable(rawSender.getPickupType())
                        .map(ParcelShippingSender.PickupTypeEnum::getValue)
                        .map(PickupType::fromWire),
                Optional.ofNullable(rawSender.getPointCode()));
    }

    private static ShippingParty toReceiver(ParcelShippingReceiver rawReceiver) {
        return new ShippingParty(
                Optional.ofNullable(rawReceiver.getFirstName()),
                Optional.ofNullable(rawReceiver.getLastName()),
                Optional.ofNullable(rawReceiver.getCompanyName()),
                Optional.ofNullable(rawReceiver.getStreet()),
                Optional.ofNullable(rawReceiver.getBuildingNumber()),
                Optional.ofNullable(rawReceiver.getFlatNumber()),
                Optional.ofNullable(rawReceiver.getCity()),
                toZip(rawReceiver.getZip(), "shipping.receiver.zip"),
                Optional.ofNullable(rawReceiver.getCountry())
                        .map(ParcelShippingReceiver.CountryEnum::getValue)
                        .map(ShippingCountry::fromWire),
                Optional.ofNullable(rawReceiver.getPhoneNumber()),
                Optional.ofNullable(rawReceiver.getEmail()),
                Optional.ofNullable(rawReceiver.getPickupType())
                        .map(ParcelShippingReceiver.PickupTypeEnum::getValue)
                        .map(PickupType::fromWire),
                Optional.ofNullable(rawReceiver.getPointCode()));
    }

    /**
     * The postcode arrives as a {@code oneOf} wrapper — the spec constrains its format per country — so
     * the generated type exposes the branch rather than a plain string. Every declared branch is a
     * string today; an unexpected one is reported rather than silently dropped, so a spec change shows
     * up as a named failure instead of a postcode quietly going missing from a delivery address.
     */
    private static Optional<String> toZip(CreateParcelsInnerShippingReceiverZip rawZip, String fieldName) {
        if (rawZip == null || rawZip.getActualInstance() == null) {
            return Optional.empty();
        }
        Object actualZip = rawZip.getActualInstance();
        if (actualZip instanceof String zipText) {
            return Optional.of(zipText);
        }
        throw new IllegalStateException("Parcel field '" + fieldName
                + "' decoded to an unexpected branch type: " + actualZip.getClass().getName());
    }
}
