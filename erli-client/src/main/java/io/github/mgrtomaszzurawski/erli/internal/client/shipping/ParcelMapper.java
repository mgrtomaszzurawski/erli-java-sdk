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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated Layer-1 {@link io.github.mgrtomaszzurawski.erli.rest.model.Parcel} onto the public
 * {@link Parcel} domain record. Kept internal so no {@code *Raw} type reaches an exported signature.
 *
 * <p>Enums are translated with exhaustive switches rather than {@code valueOf(name())}: when Erli adds
 * a parcel status or country, this file stops compiling instead of throwing at runtime on a live
 * payload. Fields the spec marks required are asserted here — a response missing one is a server
 * contract break, and failing loudly beats handing the caller a half-built record.
 */
final class ParcelMapper {

    private ParcelMapper() {
    }

    static Parcel toDomain(io.github.mgrtomaszzurawski.erli.rest.model.Parcel rawParcel) {
        Objects.requireNonNull(rawParcel, "raw Parcel");
        return new Parcel(
                Optional.ofNullable(rawParcel.getId()).map(String::valueOf).map(ParcelId::of),
                toType(rawParcel.getType()),
                Optional.ofNullable(rawParcel.getOrderId()).map(OrderId::of),
                Boolean.TRUE.equals(rawParcel.getErliPro()),
                toDimensions(requireField(rawParcel.getDimensions(), "dimensions")),
                toErrors(rawParcel.getErrors()),
                toStatus(requireField(rawParcel.getStatus(), "status")),
                toStatusHistory(rawParcel.getStatusHistory()),
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

    private static ParcelType toType(io.github.mgrtomaszzurawski.erli.rest.model.Parcel.TypeEnum rawType) {
        requireField(rawType, "type");
        return switch (rawType) {
            case INTERNAL -> ParcelType.INTERNAL;
        };
    }

    private static ParcelDimensions toDimensions(CreateParcelsInnerDimensions rawDimensions) {
        return new ParcelDimensions(
                requireField(rawDimensions.getWidth(), "dimensions.width"),
                requireField(rawDimensions.getHeight(), "dimensions.height"),
                requireField(rawDimensions.getLength(), "dimensions.length"),
                requireField(rawDimensions.getWeight(), "dimensions.weight"));
    }

    private static List<ParcelError> toErrors(List<ErrorResponseInner> rawErrors) {
        if (rawErrors == null) {
            return List.of();
        }
        return rawErrors.stream().map(ParcelMapper::toError).toList();
    }

    private static ParcelError toError(ErrorResponseInner rawError) {
        BigDecimal errorCode = requireField(rawError.getErrorCode(), "errors[].errorCode");
        return new ParcelError(errorCode.intValueExact(), Optional.ofNullable(rawError.getErrorMessage()));
    }

    private static List<ParcelStatusChange> toStatusHistory(List<ParcelStatusHistoryInner> rawHistory) {
        if (rawHistory == null) {
            return List.of();
        }
        return rawHistory.stream().map(ParcelMapper::toStatusChange).toList();
    }

    private static ParcelStatusChange toStatusChange(ParcelStatusHistoryInner rawEntry) {
        return new ParcelStatusChange(
                toHistoryStatus(requireField(rawEntry.getStatus(), "statusHistory[].status")),
                requireField(rawEntry.getChanged(), "statusHistory[].changed"));
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
                rawShipping.getWaybills() == null ? List.of() : List.copyOf(rawShipping.getWaybills()),
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
                toZip(rawSender.getZip()),
                Optional.ofNullable(rawSender.getCountry()).map(ParcelMapper::toSenderCountry),
                Optional.ofNullable(rawSender.getPhoneNumber()),
                Optional.ofNullable(rawSender.getEmail()),
                Optional.ofNullable(rawSender.getPickupType()).map(ParcelMapper::toSenderPickupType),
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
                toZip(rawReceiver.getZip()),
                Optional.ofNullable(rawReceiver.getCountry()).map(ParcelMapper::toReceiverCountry),
                Optional.ofNullable(rawReceiver.getPhoneNumber()),
                Optional.ofNullable(rawReceiver.getEmail()),
                Optional.ofNullable(rawReceiver.getPickupType()).map(ParcelMapper::toReceiverPickupType),
                Optional.ofNullable(rawReceiver.getPointCode()));
    }

    /**
     * The postcode arrives as a {@code oneOf} wrapper (the spec constrains the format per country), so
     * the generated type exposes the branch rather than a plain string. Only the string branch exists
     * today; anything else is surfaced as absent rather than guessed at.
     */
    private static Optional<String> toZip(CreateParcelsInnerShippingReceiverZip rawZip) {
        if (rawZip == null) {
            return Optional.empty();
        }
        Object actualZip = rawZip.getActualInstance();
        return actualZip instanceof String zipText ? Optional.of(zipText) : Optional.empty();
    }

    private static ShippingCountry toSenderCountry(ParcelShippingSender.CountryEnum rawCountry) {
        return switch (rawCountry) {
            case PL -> ShippingCountry.PL;
            case DE -> ShippingCountry.DE;
        };
    }

    private static ShippingCountry toReceiverCountry(ParcelShippingReceiver.CountryEnum rawCountry) {
        return switch (rawCountry) {
            case PL -> ShippingCountry.PL;
            case DE -> ShippingCountry.DE;
        };
    }

    private static PickupType toSenderPickupType(ParcelShippingSender.PickupTypeEnum rawPickupType) {
        return switch (rawPickupType) {
            case COURIER -> PickupType.COURIER;
            case POINT -> PickupType.POINT;
        };
    }

    private static PickupType toReceiverPickupType(ParcelShippingReceiver.PickupTypeEnum rawPickupType) {
        return switch (rawPickupType) {
            case COURIER -> PickupType.COURIER;
            case POINT -> PickupType.POINT;
        };
    }

    private static ParcelStatus toStatus(io.github.mgrtomaszzurawski.erli.rest.model.Parcel.StatusEnum rawStatus) {
        return switch (rawStatus) {
            case PREPARING -> ParcelStatus.PREPARING;
            case READY_TO_SEND -> ParcelStatus.READY_TO_SEND;
            case WAITING_FOR_COURIER -> ParcelStatus.WAITING_FOR_COURIER;
            case SENT -> ParcelStatus.SENT;
            case ON_THE_WAY -> ParcelStatus.ON_THE_WAY;
            case READY_TO_DELIVER -> ParcelStatus.READY_TO_DELIVER;
            case DELIVERED -> ParcelStatus.DELIVERED;
            case READY_TO_PICKUP -> ParcelStatus.READY_TO_PICKUP;
            case PICKUP_TIME_EXPIRED -> ParcelStatus.PICKUP_TIME_EXPIRED;
            case RETURNED -> ParcelStatus.RETURNED;
            case CANCELED -> ParcelStatus.CANCELED;
            case CLAIMED -> ParcelStatus.CLAIMED;
            case TRACKING_UNAVAILABLE -> ParcelStatus.TRACKING_UNAVAILABLE;
            case UNKNOWN -> ParcelStatus.UNKNOWN;
            case ERROR -> ParcelStatus.ERROR;
            case DELIVERY_UNSUCCESSFUL -> ParcelStatus.DELIVERY_UNSUCCESSFUL;
            case REDIRECTED -> ParcelStatus.REDIRECTED;
            case TECHNICAL -> ParcelStatus.TECHNICAL;
            case TRACKING_EXPIRED -> ParcelStatus.TRACKING_EXPIRED;
        };
    }

    private static ParcelStatus toHistoryStatus(ParcelStatusHistoryInner.StatusEnum rawStatus) {
        return switch (rawStatus) {
            case PREPARING -> ParcelStatus.PREPARING;
            case READY_TO_SEND -> ParcelStatus.READY_TO_SEND;
            case WAITING_FOR_COURIER -> ParcelStatus.WAITING_FOR_COURIER;
            case SENT -> ParcelStatus.SENT;
            case ON_THE_WAY -> ParcelStatus.ON_THE_WAY;
            case READY_TO_DELIVER -> ParcelStatus.READY_TO_DELIVER;
            case DELIVERED -> ParcelStatus.DELIVERED;
            case READY_TO_PICKUP -> ParcelStatus.READY_TO_PICKUP;
            case PICKUP_TIME_EXPIRED -> ParcelStatus.PICKUP_TIME_EXPIRED;
            case RETURNED -> ParcelStatus.RETURNED;
            case CANCELED -> ParcelStatus.CANCELED;
            case CLAIMED -> ParcelStatus.CLAIMED;
            case TRACKING_UNAVAILABLE -> ParcelStatus.TRACKING_UNAVAILABLE;
            case UNKNOWN -> ParcelStatus.UNKNOWN;
            case ERROR -> ParcelStatus.ERROR;
            case DELIVERY_UNSUCCESSFUL -> ParcelStatus.DELIVERY_UNSUCCESSFUL;
            case REDIRECTED -> ParcelStatus.REDIRECTED;
            case TECHNICAL -> ParcelStatus.TECHNICAL;
            case TRACKING_EXPIRED -> ParcelStatus.TRACKING_EXPIRED;
        };
    }
}
