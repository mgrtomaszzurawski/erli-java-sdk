package io.github.mgrtomaszzurawski.erli.internal.client.orders;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.orders.BankAccount;
import io.github.mgrtomaszzurawski.erli.domain.orders.Buyer;
import io.github.mgrtomaszzurawski.erli.domain.orders.Country;
import io.github.mgrtomaszzurawski.erli.domain.orders.Delivery;
import io.github.mgrtomaszzurawski.erli.domain.orders.DeliveryAddress;
import io.github.mgrtomaszzurawski.erli.domain.orders.DeliveryTracking;
import io.github.mgrtomaszzurawski.erli.domain.orders.InvoiceAddress;
import io.github.mgrtomaszzurawski.erli.domain.orders.InvoiceAddressType;
import io.github.mgrtomaszzurawski.erli.domain.orders.Order;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderItem;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderPayment;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderReturn;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderStatus;
import io.github.mgrtomaszzurawski.erli.domain.orders.PaymentStatus;
import io.github.mgrtomaszzurawski.erli.domain.orders.PickupPlace;
import io.github.mgrtomaszzurawski.erli.domain.orders.PickupProvider;
import io.github.mgrtomaszzurawski.erli.domain.orders.Rebate;
import io.github.mgrtomaszzurawski.erli.domain.orders.ReturnReason;
import io.github.mgrtomaszzurawski.erli.domain.orders.ReturnedItem;
import io.github.mgrtomaszzurawski.erli.domain.orders.SellerStatus;
import io.github.mgrtomaszzurawski.erli.domain.orders.TaxRate;
import io.github.mgrtomaszzurawski.erli.domain.orders.TrackingStatus;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderDelivery;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderDeliveryPickupPlace;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderDeliveryTracking;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderItemsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderRebate;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderReturnsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderReturnsInnerBankAccount;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderReturnsInnerItemsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUser;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUserDeliveryAddress;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUserInvoiceAddress;

import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Maps the generated Layer-1 {@code Order} and its nested types to the public {@code domain.orders}
 * records. Kept in an internal package so no {@code *Raw} type ever appears in an exported signature.
 *
 * <p>Two conversions are worth knowing about:
 * <ul>
 *   <li><strong>Money.</strong> Erli sends every amount as an integer count of minor units (grosze)
 *       with the order's currency alongside, so amounts go through the core
 *       {@link Money#ofMinorUnits} helper rather than a local divisor.</li>
 *   <li><strong>Closed enums</strong> are translated with an explicit exhaustive {@code switch}, never
 *       {@code valueOf(name())}. That decouples the public enums from the generated constant names and
 *       turns a new upstream value into a compile error here — a deliberate signal to map it — instead
 *       of a runtime surprise for a consumer.</li>
 *   <li><strong>Growing reference sets</strong> ({@link DeliveryVendor}) are mapped by wire value
 *       instead of an N-arm switch. Since CORE-12 an unrecognised carrier decodes to {@code null}
 *       rather than throwing, and lands here as an absent {@code vendor}; see the note on
 *       {@link #toDeliveryTracking}.</li>
 * </ul>
 *
 * <p>Required fields are validated: a payload missing one fails fast with a message naming the field,
 * because a half-built {@code Order} is far harder to debug downstream than an immediate error.
 */
final class OrderMapper {

    private static final String CURRENCY_PLN = "PLN";
    private static final String CURRENCY_EUR = "EUR";

    private OrderMapper() {
    }

    static Order toDomain(io.github.mgrtomaszzurawski.erli.rest.model.Order rawOrder) {
        Objects.requireNonNull(rawOrder, "raw Order");
        Currency currency = toCurrency(rawOrder);
        return new Order(
                OrderId.of(required(rawOrder.getId(), "id")),
                Optional.ofNullable(rawOrder.getExternalOrderId()),
                toOrderStatus(rawOrder),
                Optional.ofNullable(rawOrder.getUser()).map(OrderMapper::toBuyer),
                toItems(rawOrder, currency),
                Optional.ofNullable(rawOrder.getRebate()).map(OrderMapper::toRebate),
                toDelivery(required(rawOrder.getDelivery(), "delivery"), currency),
                Optional.ofNullable(rawOrder.getComment()),
                money(required(rawOrder.getTotalPrice(), "totalPrice"), currency),
                Optional.ofNullable(rawOrder.getDeliveryTracking()).map(OrderMapper::toDeliveryTracking),
                Optional.ofNullable(rawOrder.getPayment()).map(OrderMapper::toPayment),
                toReturns(rawOrder),
                optionalInt(rawOrder.getCalculatedParcelsCount()),
                toSellerStatus(rawOrder),
                required(rawOrder.getCreated(), "created"),
                required(rawOrder.getUpdated(), "updated"),
                Optional.ofNullable(rawOrder.getPurchasedAt()),
                Optional.ofNullable(rawOrder.getCursor()).map(Cursor::of));
    }

    // --- buyer ------------------------------------------------------------------------------------

    private static Buyer toBuyer(OrderUser rawUser) {
        return new Buyer(
                required(rawUser.getEmail(), "user.email"),
                toDeliveryAddress(required(rawUser.getDeliveryAddress(), "user.deliveryAddress")),
                Optional.ofNullable(rawUser.getInvoiceAddress()).map(OrderMapper::toInvoiceAddress));
    }

    private static DeliveryAddress toDeliveryAddress(OrderUserDeliveryAddress rawAddress) {
        return new DeliveryAddress(
                required(rawAddress.getFirstName(), "deliveryAddress.firstName"),
                required(rawAddress.getLastName(), "deliveryAddress.lastName"),
                Optional.ofNullable(rawAddress.getCompanyName()),
                required(rawAddress.getAddress(), "deliveryAddress.address"),
                required(rawAddress.getStreet(), "deliveryAddress.street"),
                required(rawAddress.getBuildingNumber(), "deliveryAddress.buildingNumber"),
                Optional.ofNullable(rawAddress.getFlatNumber()),
                required(rawAddress.getZip(), "deliveryAddress.zip"),
                required(rawAddress.getCity(), "deliveryAddress.city"),
                toCountry(rawAddress.getCountry()),
                required(rawAddress.getPhone(), "deliveryAddress.phone"));
    }

    private static InvoiceAddress toInvoiceAddress(OrderUserInvoiceAddress rawAddress) {
        return new InvoiceAddress(
                toInvoiceAddressType(rawAddress.getType()),
                required(rawAddress.getAddress(), "invoiceAddress.address"),
                required(rawAddress.getStreet(), "invoiceAddress.street"),
                required(rawAddress.getBuildingNumber(), "invoiceAddress.buildingNumber"),
                Optional.ofNullable(rawAddress.getFlatNumber()),
                required(rawAddress.getZip(), "invoiceAddress.zip"),
                required(rawAddress.getCity(), "invoiceAddress.city"),
                toInvoiceCountry(rawAddress.getCountry()),
                Optional.ofNullable(rawAddress.getFirstName()),
                Optional.ofNullable(rawAddress.getLastName()),
                Optional.ofNullable(rawAddress.getCompanyName()),
                Optional.ofNullable(rawAddress.getNip()));
    }

    // --- items and rebate -------------------------------------------------------------------------

    private static List<OrderItem> toItems(
            io.github.mgrtomaszzurawski.erli.rest.model.Order rawOrder, Currency currency) {
        List<OrderItemsInner> rawItems = required(rawOrder.getItems(), "items");
        return rawItems.stream().map(rawItem -> toItem(rawItem, currency)).toList();
    }

    private static OrderItem toItem(OrderItemsInner rawItem, Currency currency) {
        return new OrderItem(
                required(rawItem.getId(), "items.id").longValue(),
                ProductExternalId.of(required(rawItem.getExternalId(), "items.externalId")),
                required(rawItem.getQuantity(), "items.quantity"),
                Optional.ofNullable(rawItem.getWeight()),
                money(required(rawItem.getUnitPrice(), "items.unitPrice"), currency),
                Optional.ofNullable(rawItem.getUnitPriceBeforeRebate()).map(price -> money(price, currency)),
                required(rawItem.getName(), "items.name"),
                required(rawItem.getSlug(), "items.slug"),
                Optional.ofNullable(rawItem.getEan()),
                Optional.ofNullable(rawItem.getSku()),
                Optional.ofNullable(rawItem.getTaxRate()).map(OrderMapper::toTaxRate));
    }

    private static Rebate toRebate(OrderRebate rawRebate) {
        return new Rebate(
                required(rawRebate.getId(), "rebate.id").longValue(),
                required(rawRebate.getName(), "rebate.name"),
                Optional.ofNullable(rawRebate.getCode()));
    }

    // --- delivery ---------------------------------------------------------------------------------

    private static Delivery toDelivery(OrderDelivery rawDelivery, Currency currency) {
        return new Delivery(
                required(rawDelivery.getName(), "delivery.name"),
                DeliveryMethodId.of(required(rawDelivery.getTypeId(), "delivery.typeId")),
                money(required(rawDelivery.getPrice(), "delivery.price"), currency),
                optionalInt(rawDelivery.getCancelled()),
                required(rawDelivery.getCod(), "delivery.cod"),
                Optional.ofNullable(rawDelivery.getSourceMarket()),
                Optional.ofNullable(rawDelivery.getTargetMarket()),
                Optional.ofNullable(rawDelivery.getPickupPlace()).map(OrderMapper::toPickupPlace));
    }

    private static PickupPlace toPickupPlace(OrderDeliveryPickupPlace rawPlace) {
        return new PickupPlace(
                rawPlace.getId() == null ? OptionalLong.empty() : OptionalLong.of(rawPlace.getId().longValue()),
                Optional.ofNullable(rawPlace.getExternalId()),
                Optional.ofNullable(rawPlace.getHeading()),
                Optional.ofNullable(rawPlace.getType()),
                Optional.ofNullable(rawPlace.getProvider()).map(OrderMapper::toPickupProvider),
                Optional.ofNullable(rawPlace.getName()),
                Optional.ofNullable(rawPlace.getDescription()),
                Optional.ofNullable(rawPlace.getAddress()),
                Optional.ofNullable(rawPlace.getCity()),
                Optional.ofNullable(rawPlace.getCountry()),
                Optional.ofNullable(rawPlace.getOpen24h()),
                Optional.ofNullable(rawPlace.getZip()));
    }

    /**
     * Erli declares tracking as a choice between {@code {status, trackingUrl}} and
     * {@code {status, vendor, trackingNumber}}. Layer 1 merges the two into one object (the
     * {@code normalizeSpec} composite merge, CORE-3), so which shape arrived is simply which optional
     * fields are set.
     *
     * <p>One consequence of CORE-12 is worth knowing: the codec decodes an unrecognised enum value to
     * {@code null} instead of throwing, and {@code vendor} is optional, so a carrier Erli adds after
     * this SDK was built surfaces as an empty {@code vendor} — the same as a payload that carried no
     * carrier at all. Only the carrier's identity is lost; {@code trackingNumber} still tells the two
     * cases apart for a caller (see {@code docs/orders.md}).
     *
     * <p>{@code KNOWN-SERVER-BEHAVIORS.md} suggests an {@code UNRECOGNIZED} sentinel for growing
     * enums. <strong>It is not used for this field</strong>, because the field is optional: Jackson
     * yields {@code null} for absent and unrecognised alike, so a sentinel would relabel every
     * genuinely-absent vendor as unrecognised — worse than what it fixes. That reasoning is specific
     * to optional fields and is not a fleet-wide ruling; recovering the real value would mean reading
     * {@code vendor} from the JSON tree, filed in {@code BACKLOG.md}. {@code status}, being required,
     * still fails loudly on {@code null}.
     */
    private static DeliveryTracking toDeliveryTracking(OrderDeliveryTracking rawTracking) {
        return new DeliveryTracking(
                toTrackingStatus(rawTracking.getStatus()),
                Optional.ofNullable(rawTracking.getVendor()).map(OrderMapper::toDeliveryVendor),
                Optional.ofNullable(rawTracking.getTrackingNumber()),
                Optional.ofNullable(rawTracking.getTrackingUrl()));
    }

    // --- payment and returns ----------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private static OrderPayment toPayment(io.github.mgrtomaszzurawski.erli.rest.model.OrderPayment rawPayment) {
        return new OrderPayment(
                required(rawPayment.getId(), "payment.id").longValue(),
                Optional.ofNullable(rawPayment.getStatus()).map(OrderMapper::toPaymentStatus));
    }

    private static List<OrderReturn> toReturns(io.github.mgrtomaszzurawski.erli.rest.model.Order rawOrder) {
        List<OrderReturnsInner> rawReturns = rawOrder.getReturns();
        return rawReturns == null ? List.of() : rawReturns.stream().map(OrderMapper::toReturn).toList();
    }

    private static OrderReturn toReturn(OrderReturnsInner rawReturn) {
        List<OrderReturnsInnerItemsInner> rawItems = required(rawReturn.getItems(), "returns.items");
        return new OrderReturn(
                rawItems.stream().map(OrderMapper::toReturnedItem).toList(),
                Optional.ofNullable(rawReturn.getBankAccount()).map(OrderMapper::toBankAccount),
                toReturnReason(rawReturn.getReason()),
                Optional.ofNullable(rawReturn.getComment()),
                required(rawReturn.getCreated(), "returns.created"));
    }

    /**
     * Erli's schema spells the quantity property {@code quentity}. The typo is upstream's and is
     * preserved on the wire; the domain record uses the correct spelling.
     */
    private static ReturnedItem toReturnedItem(OrderReturnsInnerItemsInner rawItem) {
        return new ReturnedItem(
                required(rawItem.getIndex(), "returns.items.index"),
                required(rawItem.getQuentity(), "returns.items.quentity"));
    }

    private static BankAccount toBankAccount(OrderReturnsInnerBankAccount rawAccount) {
        return new BankAccount(
                required(rawAccount.getNumber(), "returns.bankAccount.number"),
                required(rawAccount.getName(), "returns.bankAccount.name"));
    }

    // --- enums ------------------------------------------------------------------------------------

    private static Currency toCurrency(io.github.mgrtomaszzurawski.erli.rest.model.Order rawOrder) {
        return switch (required(rawOrder.getCurrency(), "currency")) {
            case PLN -> Currency.getInstance(CURRENCY_PLN);
            case EUR -> Currency.getInstance(CURRENCY_EUR);
        };
    }

    private static OrderStatus toOrderStatus(io.github.mgrtomaszzurawski.erli.rest.model.Order rawOrder) {
        return switch (required(rawOrder.getStatus(), "status")) {
            case PENDING -> OrderStatus.PENDING;
            case PURCHASED -> OrderStatus.PURCHASED;
            case CANCELLED -> OrderStatus.CANCELLED;
            case RETURNED -> OrderStatus.RETURNED;
        };
    }

    private static SellerStatus toSellerStatus(io.github.mgrtomaszzurawski.erli.rest.model.Order rawOrder) {
        return switch (required(rawOrder.getSellerStatus(), "sellerStatus")) {
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
            case TAX_NP -> TaxRate.TAX_NP;
            case TAX_ZW -> TaxRate.TAX_ZW;
        };
    }

    private static Country toCountry(OrderUserDeliveryAddress.CountryEnum rawCountry) {
        return switch (required(rawCountry, "deliveryAddress.country")) {
            case PL -> Country.PL;
        };
    }

    private static Country toInvoiceCountry(OrderUserInvoiceAddress.CountryEnum rawCountry) {
        return switch (required(rawCountry, "invoiceAddress.country")) {
            case PL -> Country.PL;
        };
    }

    private static InvoiceAddressType toInvoiceAddressType(OrderUserInvoiceAddress.TypeEnum rawType) {
        return switch (required(rawType, "invoiceAddress.type")) {
            case COMPANY -> InvoiceAddressType.COMPANY;
            case PERSON -> InvoiceAddressType.PERSON;
        };
    }

    private static PickupProvider toPickupProvider(OrderDeliveryPickupPlace.ProviderEnum rawProvider) {
        return switch (rawProvider) {
            case PP -> PickupProvider.PP;
            case INPOST -> PickupProvider.INPOST;
            case RUCH -> PickupProvider.RUCH;
            case DPD -> PickupProvider.DPD;
            case UPS -> PickupProvider.UPS;
            case DHL -> PickupProvider.DHL;
        };
    }

    private static TrackingStatus toTrackingStatus(OrderDeliveryTracking.StatusEnum rawStatus) {
        return switch (required(rawStatus, "deliveryTracking.status")) {
            case PREPARING -> TrackingStatus.PREPARING;
            case READY_TO_SEND -> TrackingStatus.READY_TO_SEND;
            case WAITING_FOR_COURIER -> TrackingStatus.WAITING_FOR_COURIER;
            case SENT -> TrackingStatus.SENT;
            case ON_THE_WAY -> TrackingStatus.ON_THE_WAY;
            case READY_TO_PICKUP -> TrackingStatus.READY_TO_PICKUP;
            case RETURNED -> TrackingStatus.RETURNED;
            case CANCELED -> TrackingStatus.CANCELED;
            case TRACKING_UNAVAILABLE -> TrackingStatus.TRACKING_UNAVAILABLE;
        };
    }

    /**
     * Mapped by wire value rather than an exhaustive switch: the carrier list is a growing reference
     * set, the fleet convention for which is a {@code fromWire} lookup (see
     * {@code KNOWN-SERVER-BEHAVIORS.md}). The type is core's, shared with Comms and Dictionaries
     * (CORE-7). Shipping still carries its own {@code domain.shipping.ShippingVendor}; folding that
     * one in is bucket C's follow-up, not something to work around here.
     */
    private static DeliveryVendor toDeliveryVendor(OrderDeliveryTracking.VendorEnum rawVendor) {
        return DeliveryVendor.fromWire(rawVendor.getValue());
    }

    @SuppressWarnings("deprecation")
    private static PaymentStatus toPaymentStatus(
            io.github.mgrtomaszzurawski.erli.rest.model.OrderPayment.StatusEnum rawStatus) {
        return switch (rawStatus) {
            case NEW -> PaymentStatus.NEW;
            case PENDING -> PaymentStatus.PENDING;
            case WAITING_FOR_CONFIRMATION -> PaymentStatus.WAITING_FOR_CONFIRMATION;
            case COMPLETED -> PaymentStatus.COMPLETED;
            case CANCELED -> PaymentStatus.CANCELED;
        };
    }

    private static ReturnReason toReturnReason(OrderReturnsInner.ReasonEnum rawReason) {
        return switch (required(rawReason, "returns.reason")) {
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

    // --- primitives -------------------------------------------------------------------------------

    /** Erli sends amounts as integer minor units (grosze); core owns the conversion. */
    private static Money money(Integer minorUnits, Currency currency) {
        return Money.ofMinorUnits(minorUnits.longValue(), currency);
    }

    private static OptionalInt optionalInt(Integer value) {
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    /**
     * A response that omits a field the spec marks required is a server contract violation, so it is
     * reported as {@link ErliTransportException} — part of the documented {@code ErliException}
     * taxonomy — rather than as a bare {@code IllegalStateException} a caller would have to catch
     * separately from every other API failure. The message names the field; it never carries the value,
     * which could be buyer personal data.
     */
    private static <T> T required(T value, String fieldName) {
        if (value == null) {
            throw new ErliTransportException(
                    "Order response is missing the required '" + fieldName + "' field");
        }
        return value;
    }
}
