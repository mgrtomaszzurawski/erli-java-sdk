package io.github.mgrtomaszzurawski.erli.internal.client.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.inbox.BankAccount;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Buyer;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Country;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Delivery;
import io.github.mgrtomaszzurawski.erli.domain.inbox.DeliveryAddress;
import io.github.mgrtomaszzurawski.erli.domain.inbox.DeliveryTracking;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.domain.inbox.InvoiceAddress;
import io.github.mgrtomaszzurawski.erli.domain.inbox.InvoiceAddressType;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderEvent;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderLine;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderPaymentSummary;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderReturn;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderStatus;
import io.github.mgrtomaszzurawski.erli.domain.inbox.PaymentStatus;
import io.github.mgrtomaszzurawski.erli.domain.inbox.PickupPlace;
import io.github.mgrtomaszzurawski.erli.domain.inbox.PickupProvider;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Rebate;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ReturnReason;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ReturnedLine;
import io.github.mgrtomaszzurawski.erli.domain.inbox.SellerStatus;
import io.github.mgrtomaszzurawski.erli.domain.inbox.TaxRate;
import io.github.mgrtomaszzurawski.erli.domain.inbox.TrackingStatus;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.MessagePayloadAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderDelivery;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderDeliveryPickupPlace;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderDeliveryTrackingAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderDeliveryTrackingAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderItemsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderPayment;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderRebate;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderReturnsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderReturnsInnerBankAccount;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderReturnsInnerItemsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUser;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUserDeliveryAddress;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUserInvoiceAddress;

import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the order snapshot an {@code ORDER_*} inbox message carries to the {@link OrderEvent} domain
 * record and its nested records. Internal.
 *
 * <p>Two things are worth knowing about the shape:
 * <ul>
 *   <li>All money in the payload arrives as an <strong>integer count of minor units</strong> (grosze
 *       for {@code PLN}); the order-level {@code currency} applies to every amount in the payload, and
 *       {@link Money#ofMinorUnits} rebuilds it at that currency's own scale.</li>
 *   <li>{@code deliveryTracking} is declared as two alternative shapes ({@code trackingUrl} versus
 *       {@code vendor} + {@code trackingNumber}). The generated {@code anyOf} wrapper cannot pick
 *       between them under a lenient mapper, so the tracking subtree is bound to both branches and the
 *       fields that are present are merged — see {@link #toDeliveryTracking}.</li>
 * </ul>
 */
final class OrderEventMapper {

    private static final String FIELD_DELIVERY_TRACKING = "deliveryTracking";
    private static final String CURRENCY_CODE_PLN = "PLN";
    private static final String CURRENCY_CODE_EUR = "EUR";

    private OrderEventMapper() {
    }

    /**
     * @param rawPayload  the payload bound to the order branch of the {@code anyOf}
     * @param payloadNode the same payload as a tree, needed for the ambiguous tracking sub-object
     * @param codec       binds the tracking subtree to its two candidate branches
     */
    static OrderEvent toDomain(MessagePayloadAnyOf rawPayload, JsonNode payloadNode, JsonCodec codec) {
        Objects.requireNonNull(rawPayload, "raw order payload");
        Currency currency = toCurrency(rawPayload);
        return new OrderEvent(
                OrderId.of(requireText(rawPayload.getId(), "payload.id")),
                Optional.ofNullable(rawPayload.getExternalOrderId()),
                toOrderStatus(rawPayload),
                Optional.ofNullable(rawPayload.getUser()).map(OrderEventMapper::toBuyer),
                toLines(rawPayload.getItems(), currency),
                Optional.ofNullable(rawPayload.getRebate()).map(OrderEventMapper::toRebate),
                toDelivery(requireDelivery(rawPayload), currency),
                Optional.ofNullable(rawPayload.getComment()),
                Money.ofMinorUnits(requireInteger(rawPayload.getTotalPrice(), "payload.totalPrice"), currency),
                currency,
                toDeliveryTracking(payloadNode, codec),
                Optional.ofNullable(rawPayload.getPayment()).map(OrderEventMapper::toPaymentSummary),
                toReturns(rawPayload.getReturns()),
                Optional.ofNullable(rawPayload.getCalculatedParcelsCount()),
                toSellerStatus(rawPayload),
                requireTimestamp(rawPayload.getCreated(), "payload.created"),
                requireTimestamp(rawPayload.getUpdated(), "payload.updated"),
                Optional.ofNullable(rawPayload.getPurchasedAt()),
                Optional.ofNullable(rawPayload.getCursor()).map(Cursor::of));
    }

    private static Buyer toBuyer(OrderUser rawUser) {
        return new Buyer(
                requireText(rawUser.getEmail(), "payload.user.email"),
                toDeliveryAddress(requireDeliveryAddress(rawUser)),
                Optional.ofNullable(rawUser.getInvoiceAddress()).map(OrderEventMapper::toInvoiceAddress));
    }

    private static DeliveryAddress toDeliveryAddress(OrderUserDeliveryAddress rawAddress) {
        return new DeliveryAddress(
                requireText(rawAddress.getFirstName(), "deliveryAddress.firstName"),
                requireText(rawAddress.getLastName(), "deliveryAddress.lastName"),
                Optional.ofNullable(rawAddress.getCompanyName()),
                requireText(rawAddress.getAddress(), "deliveryAddress.address"),
                requireText(rawAddress.getStreet(), "deliveryAddress.street"),
                requireText(rawAddress.getBuildingNumber(), "deliveryAddress.buildingNumber"),
                Optional.ofNullable(rawAddress.getFlatNumber()),
                requireText(rawAddress.getZip(), "deliveryAddress.zip"),
                requireText(rawAddress.getCity(), "deliveryAddress.city"),
                toCountry(rawAddress.getCountry()),
                requireText(rawAddress.getPhone(), "deliveryAddress.phone"));
    }

    private static InvoiceAddress toInvoiceAddress(OrderUserInvoiceAddress rawAddress) {
        return new InvoiceAddress(
                toInvoiceAddressType(rawAddress.getType()),
                requireText(rawAddress.getAddress(), "invoiceAddress.address"),
                requireText(rawAddress.getStreet(), "invoiceAddress.street"),
                requireText(rawAddress.getBuildingNumber(), "invoiceAddress.buildingNumber"),
                Optional.ofNullable(rawAddress.getFlatNumber()),
                requireText(rawAddress.getZip(), "invoiceAddress.zip"),
                requireText(rawAddress.getCity(), "invoiceAddress.city"),
                toInvoiceCountry(rawAddress.getCountry()),
                Optional.ofNullable(rawAddress.getFirstName()),
                Optional.ofNullable(rawAddress.getLastName()),
                Optional.ofNullable(rawAddress.getCompanyName()),
                Optional.ofNullable(rawAddress.getNip()));
    }

    private static List<OrderLine> toLines(List<OrderItemsInner> rawItems, Currency currency) {
        if (rawItems == null) {
            throw new IllegalStateException("order payload is missing the required 'items' field");
        }
        return rawItems.stream().map(rawItem -> toLine(rawItem, currency)).toList();
    }

    private static OrderLine toLine(OrderItemsInner rawItem, Currency currency) {
        return new OrderLine(
                requireInteger(rawItem.getId(), "items[].id"),
                ProductExternalId.of(requireText(rawItem.getExternalId(), "items[].externalId")),
                requireInteger(rawItem.getQuantity(), "items[].quantity"),
                Optional.ofNullable(rawItem.getWeight()),
                Money.ofMinorUnits(requireInteger(rawItem.getUnitPrice(), "items[].unitPrice"), currency),
                Optional.ofNullable(rawItem.getUnitPriceBeforeRebate())
                        .map(minorUnits -> Money.ofMinorUnits(minorUnits, currency)),
                requireText(rawItem.getName(), "items[].name"),
                requireText(rawItem.getSlug(), "items[].slug"),
                Optional.ofNullable(rawItem.getEan()),
                Optional.ofNullable(rawItem.getSku()),
                Optional.ofNullable(rawItem.getTaxRate()).map(OrderEventMapper::toTaxRate));
    }

    private static Rebate toRebate(OrderRebate rawRebate) {
        return new Rebate(
                requireInteger(rawRebate.getId(), "rebate.id"),
                requireText(rawRebate.getName(), "rebate.name"),
                Optional.ofNullable(rawRebate.getCode()));
    }

    private static Delivery toDelivery(OrderDelivery rawDelivery, Currency currency) {
        return new Delivery(
                requireText(rawDelivery.getName(), "delivery.name"),
                DeliveryMethodId.of(requireText(rawDelivery.getTypeId(), "delivery.typeId")),
                Money.ofMinorUnits(requireInteger(rawDelivery.getPrice(), "delivery.price"), currency),
                Optional.ofNullable(rawDelivery.getCancelled()).map(minorUnits -> Money.ofMinorUnits(minorUnits, currency)),
                requireBoolean(rawDelivery.getCod(), "delivery.cod"),
                Optional.ofNullable(rawDelivery.getSourceMarket()),
                Optional.ofNullable(rawDelivery.getTargetMarket()),
                Optional.ofNullable(rawDelivery.getPickupPlace()).map(OrderEventMapper::toPickupPlace));
    }

    private static PickupPlace toPickupPlace(OrderDeliveryPickupPlace rawPlace) {
        return new PickupPlace(
                Optional.ofNullable(rawPlace.getId()).map(Integer::longValue),
                Optional.ofNullable(rawPlace.getExternalId()),
                Optional.ofNullable(rawPlace.getHeading()),
                Optional.ofNullable(rawPlace.getType()),
                Optional.ofNullable(rawPlace.getProvider()).map(OrderEventMapper::toPickupProvider),
                Optional.ofNullable(rawPlace.getName()),
                Optional.ofNullable(rawPlace.getDescription()),
                Optional.ofNullable(rawPlace.getAddress()),
                Optional.ofNullable(rawPlace.getCity()),
                Optional.ofNullable(rawPlace.getCountry()),
                Optional.ofNullable(rawPlace.getOpen24h()),
                Optional.ofNullable(rawPlace.getZip()));
    }

    /**
     * The two declared tracking shapes share only {@code status}, and the generated {@code anyOf}
     * wrapper always binds the first branch when unknown properties are ignored — which would silently
     * drop {@code vendor} and {@code trackingNumber}. Binding the subtree to both branches and merging
     * what is present is correct for either shape.
     */
    private static Optional<DeliveryTracking> toDeliveryTracking(JsonNode payloadNode, JsonCodec codec) {
        if (payloadNode == null) {
            return Optional.empty();
        }
        JsonNode trackingNode = payloadNode.get(FIELD_DELIVERY_TRACKING);
        if (trackingNode == null || trackingNode.isNull()) {
            return Optional.empty();
        }
        OrderDeliveryTrackingAnyOf urlShape = codec.convert(trackingNode, OrderDeliveryTrackingAnyOf.class);
        OrderDeliveryTrackingAnyOf1 carrierShape =
                codec.convert(trackingNode, OrderDeliveryTrackingAnyOf1.class);
        return Optional.of(new DeliveryTracking(
                toTrackingStatus(urlShape, carrierShape),
                Optional.ofNullable(urlShape.getTrackingUrl()),
                Optional.ofNullable(carrierShape.getVendor()).map(OrderEventMapper::toDeliveryVendor),
                Optional.ofNullable(carrierShape.getTrackingNumber())));
    }

    @SuppressWarnings("deprecation")
    private static OrderPaymentSummary toPaymentSummary(OrderPayment rawPayment) {
        return new OrderPaymentSummary(
                requireInteger(rawPayment.getId(), "payment.id"),
                Optional.ofNullable(rawPayment.getStatus()).map(OrderEventMapper::toPaymentStatus));
    }

    private static List<OrderReturn> toReturns(List<OrderReturnsInner> rawReturns) {
        if (rawReturns == null) {
            return List.of();
        }
        return rawReturns.stream().map(OrderEventMapper::toReturn).toList();
    }

    private static OrderReturn toReturn(OrderReturnsInner rawReturn) {
        return new OrderReturn(
                toReturnedLines(rawReturn.getItems()),
                Optional.ofNullable(rawReturn.getBankAccount()).map(OrderEventMapper::toBankAccount),
                toReturnReason(rawReturn.getReason()),
                Optional.ofNullable(rawReturn.getComment()),
                requireTimestamp(rawReturn.getCreated(), "returns[].created"));
    }

    private static List<ReturnedLine> toReturnedLines(List<OrderReturnsInnerItemsInner> rawItems) {
        if (rawItems == null) {
            throw new IllegalStateException("order return is missing the required 'items' field");
        }
        // The spec spells this property 'quentity'; the domain uses the correct English word.
        return rawItems.stream()
                .map(rawItem -> new ReturnedLine(
                        requireInteger(rawItem.getIndex(), "returns[].items[].index"),
                        requireInteger(rawItem.getQuentity(), "returns[].items[].quentity")))
                .toList();
    }

    private static BankAccount toBankAccount(OrderReturnsInnerBankAccount rawAccount) {
        return new BankAccount(
                requireText(rawAccount.getNumber(), "bankAccount.number"),
                requireText(rawAccount.getName(), "bankAccount.name"));
    }

    private static Currency toCurrency(MessagePayloadAnyOf rawPayload) {
        MessagePayloadAnyOf.CurrencyEnum rawCurrency = rawPayload.getCurrency();
        if (rawCurrency == null) {
            throw new IllegalStateException("order payload is missing the required 'currency' field");
        }
        return switch (rawCurrency) {
            case PLN -> Currency.getInstance(CURRENCY_CODE_PLN);
            case EUR -> Currency.getInstance(CURRENCY_CODE_EUR);
        };
    }

    private static OrderStatus toOrderStatus(MessagePayloadAnyOf rawPayload) {
        MessagePayloadAnyOf.StatusEnum rawStatus = rawPayload.getStatus();
        if (rawStatus == null) {
            throw new IllegalStateException("order payload is missing the required 'status' field");
        }
        return switch (rawStatus) {
            case PENDING -> OrderStatus.PENDING;
            case PURCHASED -> OrderStatus.PURCHASED;
            case CANCELLED -> OrderStatus.CANCELLED;
            case RETURNED -> OrderStatus.RETURNED;
        };
    }

    private static SellerStatus toSellerStatus(MessagePayloadAnyOf rawPayload) {
        MessagePayloadAnyOf.SellerStatusEnum rawStatus = rawPayload.getSellerStatus();
        if (rawStatus == null) {
            throw new IllegalStateException("order payload is missing the required 'sellerStatus' field");
        }
        return switch (rawStatus) {
            case CREATED -> SellerStatus.CREATED;
            case CANCELED -> SellerStatus.CANCELED;
            case READY_TO_PROCESS -> SellerStatus.READY_TO_PROCESS;
            case IN_PROGRESS -> SellerStatus.IN_PROGRESS;
            case SENT -> SellerStatus.SENT;
            case READY_TO_PICKUP -> SellerStatus.READY_TO_PICKUP;
            case RECEIVED -> SellerStatus.RECEIVED;
            case RETURNED -> SellerStatus.RETURNED;
            case RETURNING_TO_SENDER -> SellerStatus.RETURNING_TO_SENDER;
            case UNKNOWN -> SellerStatus.UNKNOWN;
        };
    }

    private static TaxRate toTaxRate(OrderItemsInner.TaxRateEnum rawTaxRate) {
        return switch (rawTaxRate) {
            case TAX_0 -> TaxRate.TAX_0;
            case TAX_5 -> TaxRate.TAX_5;
            case TAX_7 -> TaxRate.TAX_7;
            case TAX_8 -> TaxRate.TAX_8;
            case TAX_19 -> TaxRate.TAX_19;
            case TAX_23 -> TaxRate.TAX_23;
            case TAX_NP -> TaxRate.TAX_NOT_APPLICABLE;
            case TAX_ZW -> TaxRate.TAX_EXEMPT;
        };
    }

    private static Country toCountry(OrderUserDeliveryAddress.CountryEnum rawCountry) {
        if (rawCountry == null) {
            throw new IllegalStateException("deliveryAddress is missing the required 'country' field");
        }
        return switch (rawCountry) {
            case PL -> Country.PL;
        };
    }

    private static Country toInvoiceCountry(OrderUserInvoiceAddress.CountryEnum rawCountry) {
        if (rawCountry == null) {
            throw new IllegalStateException("invoiceAddress is missing the required 'country' field");
        }
        return switch (rawCountry) {
            case PL -> Country.PL;
        };
    }

    private static InvoiceAddressType toInvoiceAddressType(OrderUserInvoiceAddress.TypeEnum rawType) {
        if (rawType == null) {
            throw new IllegalStateException("invoiceAddress is missing the required 'type' field");
        }
        return switch (rawType) {
            case COMPANY -> InvoiceAddressType.COMPANY;
            case PERSON -> InvoiceAddressType.PERSON;
        };
    }

    private static PickupProvider toPickupProvider(OrderDeliveryPickupPlace.ProviderEnum rawProvider) {
        return switch (rawProvider) {
            case PP -> PickupProvider.ERLI_PICKUP_POINT;
            case INPOST -> PickupProvider.INPOST;
            case RUCH -> PickupProvider.RUCH;
            case DPD -> PickupProvider.DPD;
            case UPS -> PickupProvider.UPS;
            case DHL -> PickupProvider.DHL;
        };
    }

    private static TrackingStatus toTrackingStatus(
            OrderDeliveryTrackingAnyOf urlShape, OrderDeliveryTrackingAnyOf1 carrierShape) {
        if (urlShape.getStatus() != null) {
            return toTrackingStatus(urlShape.getStatus());
        }
        if (carrierShape.getStatus() != null) {
            return toTrackingStatus(carrierShape.getStatus());
        }
        throw new IllegalStateException("deliveryTracking is missing the required 'status' field");
    }

    private static TrackingStatus toTrackingStatus(OrderDeliveryTrackingAnyOf.StatusEnum rawStatus) {
        return switch (rawStatus) {
            case PREPARING -> TrackingStatus.PREPARING;
            case WAITING_FOR_COURIER -> TrackingStatus.WAITING_FOR_COURIER;
            case SENT -> TrackingStatus.SENT;
            case READY_TO_PICKUP -> TrackingStatus.READY_TO_PICKUP;
            case ON_THE_WAY -> TrackingStatus.ON_THE_WAY;
            case READY_TO_SEND -> TrackingStatus.READY_TO_SEND;
            case TRACKING_UNAVAILABLE -> TrackingStatus.TRACKING_UNAVAILABLE;
            case RETURNED -> TrackingStatus.RETURNED;
            case CANCELED -> TrackingStatus.CANCELED;
        };
    }

    private static TrackingStatus toTrackingStatus(OrderDeliveryTrackingAnyOf1.StatusEnum rawStatus) {
        return switch (rawStatus) {
            case PREPARING -> TrackingStatus.PREPARING;
            case WAITING_FOR_COURIER -> TrackingStatus.WAITING_FOR_COURIER;
            case SENT -> TrackingStatus.SENT;
            case READY_TO_PICKUP -> TrackingStatus.READY_TO_PICKUP;
            case ON_THE_WAY -> TrackingStatus.ON_THE_WAY;
            case READY_TO_SEND -> TrackingStatus.READY_TO_SEND;
            case TRACKING_UNAVAILABLE -> TrackingStatus.TRACKING_UNAVAILABLE;
            case RETURNED -> TrackingStatus.RETURNED;
            case CANCELED -> TrackingStatus.CANCELED;
        };
    }

    /**
     * Mapped by wire value rather than a 26-arm switch: this is a large, growing reference enum, and
     * the fleet convention is a {@code fromWire} lookup for those (see {@code KNOWN-SERVER-BEHAVIORS.md}).
     * An unknown carrier already fails earlier, when the generated enum decodes it.
     */
    private static DeliveryVendor toDeliveryVendor(OrderDeliveryTrackingAnyOf1.VendorEnum rawVendor) {
        return DeliveryVendor.fromWire(rawVendor.getValue());
    }

    private static PaymentStatus toPaymentStatus(OrderPayment.StatusEnum rawStatus) {
        return switch (rawStatus) {
            case NEW -> PaymentStatus.NEW;
            case PENDING -> PaymentStatus.PENDING;
            case WAITING_FOR_CONFIRMATION -> PaymentStatus.WAITING_FOR_CONFIRMATION;
            case COMPLETED -> PaymentStatus.COMPLETED;
            case CANCELED -> PaymentStatus.CANCELED;
        };
    }

    private static ReturnReason toReturnReason(OrderReturnsInner.ReasonEnum rawReason) {
        if (rawReason == null) {
            throw new IllegalStateException("order return is missing the required 'reason' field");
        }
        return switch (rawReason) {
            case RESIGN -> ReturnReason.RESIGN;
            case MISTAKE -> ReturnReason.MISTAKE;
            case ITEMS_QUALITY -> ReturnReason.ITEMS_QUALITY;
            case ITEMS_DESCRIPTION -> ReturnReason.ITEMS_DESCRIPTION;
            case DELIVERY_QUALITY -> ReturnReason.DELIVERY_QUALITY;
            case DOES_NOT_FIT -> ReturnReason.DOES_NOT_FIT;
            case ITEM_IS_MISSING -> ReturnReason.ITEM_IS_MISSING;
            case NOT_DELIVERED_ON_TIME -> ReturnReason.NOT_DELIVERED_ON_TIME;
            case ITEM_DAMAGED -> ReturnReason.ITEM_DAMAGED;
            case ITEM_AND_PACKAGE_DAMAGED -> ReturnReason.ITEM_AND_PACKAGE_DAMAGED;
            case OTHER -> ReturnReason.OTHER;
        };
    }

    private static OrderDelivery requireDelivery(MessagePayloadAnyOf rawPayload) {
        OrderDelivery rawDelivery = rawPayload.getDelivery();
        if (rawDelivery == null) {
            throw new IllegalStateException("order payload is missing the required 'delivery' field");
        }
        return rawDelivery;
    }

    private static OrderUserDeliveryAddress requireDeliveryAddress(OrderUser rawUser) {
        OrderUserDeliveryAddress rawAddress = rawUser.getDeliveryAddress();
        if (rawAddress == null) {
            throw new IllegalStateException("order user is missing the required 'deliveryAddress' field");
        }
        return rawAddress;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("order payload is missing the required '" + field + "' field");
        }
        return value;
    }

    private static int requireInteger(Integer value, String field) {
        if (value == null) {
            throw new IllegalStateException("order payload is missing the required '" + field + "' field");
        }
        return value;
    }

    private static boolean requireBoolean(Boolean value, String field) {
        if (value == null) {
            throw new IllegalStateException("order payload is missing the required '" + field + "' field");
        }
        return value;
    }

    private static OffsetDateTime requireTimestamp(OffsetDateTime value, String field) {
        if (value == null) {
            throw new IllegalStateException("order payload is missing the required '" + field + "' field");
        }
        return value;
    }
}
