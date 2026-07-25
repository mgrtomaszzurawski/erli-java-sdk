package io.github.mgrtomaszzurawski.erli.internal.client.orders;

import io.github.mgrtomaszzurawski.erli.core.error.ErliException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.domain.orders.Buyer;
import io.github.mgrtomaszzurawski.erli.domain.orders.Country;
import io.github.mgrtomaszzurawski.erli.domain.orders.Delivery;
import io.github.mgrtomaszzurawski.erli.domain.orders.DeliveryAddress;
import io.github.mgrtomaszzurawski.erli.domain.orders.DeliveryTracking;
import io.github.mgrtomaszzurawski.erli.domain.orders.InvoiceAddress;
import io.github.mgrtomaszzurawski.erli.domain.orders.InvoiceAddressType;
import io.github.mgrtomaszzurawski.erli.domain.orders.Order;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderItem;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderReturn;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderStatus;
import io.github.mgrtomaszzurawski.erli.domain.orders.PaymentStatus;
import io.github.mgrtomaszzurawski.erli.domain.orders.PickupPlace;
import io.github.mgrtomaszzurawski.erli.domain.orders.PickupProvider;
import io.github.mgrtomaszzurawski.erli.domain.orders.Rebate;
import io.github.mgrtomaszzurawski.erli.domain.orders.ReturnReason;
import io.github.mgrtomaszzurawski.erli.domain.orders.SellerStatus;
import io.github.mgrtomaszzurawski.erli.domain.orders.TaxRate;
import io.github.mgrtomaszzurawski.erli.domain.orders.TrackingStatus;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Field-level mapping contract for {@link OrderMapper}.
 *
 * <p><strong>Fixture provenance.</strong> The sandbox shop is empty and inactive, so no real order
 * payload can be observed yet — {@code POST /orders/_search} answers {@code 200} with an empty array
 * (see {@code KNOWN-SERVER-BEHAVIORS.md}). The fixtures are therefore built from the vendored
 * {@code openapi/swagger.json} {@code Order} schema: {@code order-full.json} populates every property
 * at every nesting level, {@code order-minimal.json} only the nine required ones. The cursor uses the
 * documented {@code unixTimestamp;idN} form rather than an invented one. They are to be replaced with
 * a captured payload during the Phase 3 live write-read sweep.
 *
 * <p>These assert fields rather than "no exception was thrown": a facade call that succeeds proves
 * nothing about whether the deep payload was actually mapped.
 */
class OrderMapperTest {

    private static final Currency PLN = Currency.getInstance("PLN");
    private static final String FULL_FIXTURE = "fixtures/orders/order-full.json";
    private static final String MINIMAL_FIXTURE = "fixtures/orders/order-minimal.json";

    private final JsonCodec codec = new JsonCodec();

    private Order mapFixture(String resourcePath) {
        return OrderMapper.toDomain(
                codec.read(TestFixtures.read(resourcePath),
                        io.github.mgrtomaszzurawski.erli.rest.model.Order.class));
    }

    @Test
    void mapsScalarsAndStatuses() {
        Order order = mapFixture(FULL_FIXTURE);

        assertEquals("221201x12345", order.id().value());
        assertEquals("erp-1001", order.externalOrderId().orElseThrow());
        assertEquals(OrderStatus.PURCHASED, order.status());
        assertEquals(SellerStatus.SENT, order.sellerStatus());
        assertEquals("Prosze zapakowac na prezent", order.comment().orElseThrow());
        assertEquals(2, order.calculatedParcelsCount().orElseThrow());
        assertEquals(OffsetDateTime.of(2026, 7, 20, 8, 15, 30, 0, ZoneOffset.UTC), order.created());
        assertEquals(OffsetDateTime.of(2026, 7, 22, 14, 5, 0, 0, ZoneOffset.UTC), order.updated());
        assertEquals(OffsetDateTime.of(2026, 7, 20, 8, 20, 0, 0, ZoneOffset.UTC),
                order.purchasedAt().orElseThrow());
        assertEquals("1753178730;221201x12345", order.cursor().orElseThrow().value());
    }

    @Test
    void rebuildsAmountsFromMinorUnitsAtTheCurrencyScale() {
        Order order = mapFixture(FULL_FIXTURE);

        // 12787 grosze is 127.87 PLN — the scale comes from the currency, not a hard-coded divisor.
        assertEquals(new BigDecimal("127.87"), order.totalPrice().amount());
        assertEquals(PLN, order.totalPrice().currency());
        assertEquals(new BigDecimal("12.90"), order.delivery().price().amount());
        assertEquals(new BigDecimal("49.99"), order.items().get(0).unitPrice().amount());
        assertEquals(new BigDecimal("59.99"), order.items().get(0).unitPriceBeforeRebate().orElseThrow().amount());
    }

    @Test
    void mapsEveryItemField() {
        OrderItem first = mapFixture(FULL_FIXTURE).items().get(0);

        assertEquals(987654L, first.id());
        assertEquals("SKU-RED-42", first.externalId().value());
        assertEquals(2, first.quantity());
        assertEquals(new BigDecimal("1.25"), first.weight().orElseThrow());
        assertEquals("Czerwony kubek", first.name());
        assertEquals("czerwony-kubek", first.slug());
        assertEquals("5901234123457", first.ean().orElseThrow());
        assertEquals("RED-42", first.sku().orElseThrow());
        assertEquals(TaxRate.TAX_23, first.taxRate().orElseThrow());
    }

    @Test
    void leavesOptionalItemFieldsEmptyWhenAbsent() {
        OrderItem second = mapFixture(FULL_FIXTURE).items().get(1);

        assertTrue(second.weight().isEmpty());
        assertTrue(second.ean().isEmpty());
        assertTrue(second.sku().isEmpty());
        assertTrue(second.taxRate().isEmpty());
        assertTrue(second.unitPriceBeforeRebate().isEmpty());
    }

    @Test
    void mapsBuyerWithBothAddresses() {
        Buyer buyer = mapFixture(FULL_FIXTURE).buyer().orElseThrow();

        assertEquals("buyer-221201x12345@proxy.erli.pl", buyer.email());

        DeliveryAddress delivery = buyer.deliveryAddress();
        assertEquals("Anna", delivery.firstName());
        assertEquals("Kowalska", delivery.lastName());
        assertEquals("Kowalska Sp. z o.o.", delivery.companyName().orElseThrow());
        assertEquals("Kwiatowa 12/3", delivery.address());
        assertEquals("Kwiatowa", delivery.street());
        assertEquals("12", delivery.buildingNumber());
        assertEquals("3", delivery.flatNumber().orElseThrow());
        assertEquals("00-950", delivery.postalCode());
        assertEquals("Warszawa", delivery.city());
        assertEquals(Country.PL, delivery.country());
        assertEquals("600100200", delivery.phone());

        InvoiceAddress invoice = buyer.invoiceAddress().orElseThrow();
        assertEquals(InvoiceAddressType.COMPANY, invoice.type());
        assertEquals("Fabryczna 7", invoice.address());
        assertEquals("Fabryczna", invoice.street());
        assertEquals("7", invoice.buildingNumber());
        assertEquals("2", invoice.flatNumber().orElseThrow());
        assertEquals("31-553", invoice.postalCode());
        assertEquals("Krakow", invoice.city());
        assertEquals(Country.PL, invoice.country());
        assertEquals("Anna", invoice.firstName().orElseThrow());
        assertEquals("Kowalska", invoice.lastName().orElseThrow());
        assertEquals("Kowalska Sp. z o.o.", invoice.companyName().orElseThrow());
        assertEquals("1234563218", invoice.taxIdentificationNumber().orElseThrow());
    }

    @Test
    void redactsBuyerPersonalDataInStringRenderings() {
        Order order = mapFixture(FULL_FIXTURE);
        Buyer buyer = order.buyer().orElseThrow();

        assertEquals("Buyer[REDACTED]", buyer.toString());
        assertEquals("DeliveryAddress[REDACTED]", buyer.deliveryAddress().toString());
        assertEquals("InvoiceAddress[REDACTED]", buyer.invoiceAddress().orElseThrow().toString());
        assertEquals("BankAccount[REDACTED]",
                order.returns().get(0).bankAccount().orElseThrow().toString());

        // The whole order is the realistic leak path: someone logs it, and the buyer's name, address,
        // phone and bank account go with it.
        String rendered = order.toString();
        assertFalse(rendered.contains("Kowalska"), "buyer surname leaked into Order.toString()");
        assertFalse(rendered.contains("600100200"), "buyer phone leaked into Order.toString()");
        assertFalse(rendered.contains("proxy.erli.pl"), "buyer e-mail leaked into Order.toString()");
        assertFalse(rendered.contains("12345678901234567890123456"),
                "bank account number leaked into Order.toString()");
    }

    @Test
    void mapsDeliveryAndPickupPlace() {
        Delivery delivery = mapFixture(FULL_FIXTURE).delivery();

        assertEquals("InPost Paczkomat", delivery.name());
        assertEquals("inpost-locker", delivery.typeId().value());
        assertEquals(0, delivery.cancelled().orElseThrow());
        assertTrue(delivery.cashOnDelivery());
        assertEquals("pl", delivery.sourceMarket().orElseThrow());
        assertEquals("pl", delivery.targetMarket().orElseThrow());

        PickupPlace place = delivery.pickupPlace().orElseThrow();
        assertEquals(4242L, place.id().orElseThrow());
        assertEquals("KRA01M", place.externalId().orElseThrow());
        assertEquals("Paczkomat KRA01M", place.heading().orElseThrow());
        assertEquals("locker", place.type().orElseThrow());
        assertEquals(PickupProvider.INPOST, place.provider().orElseThrow());
        assertEquals("KRA01M", place.name().orElseThrow());
        assertEquals("Przy wejsciu do sklepu", place.description().orElseThrow());
        assertEquals("Fabryczna 7", place.address().orElseThrow());
        assertEquals("Krakow", place.city().orElseThrow());
        assertEquals("pl", place.country().orElseThrow());
        assertEquals(Boolean.TRUE, place.open24h().orElseThrow());
        assertEquals("31-553", place.postalCode().orElseThrow());
    }

    @Test
    void mapsTheTrackedConsignmentShapeOfDeliveryTracking() {
        DeliveryTracking tracking = mapFixture(FULL_FIXTURE).deliveryTracking().orElseThrow();

        assertEquals(TrackingStatus.ON_THE_WAY, tracking.status());
        assertEquals(DeliveryVendor.INPOST, tracking.vendor().orElseThrow());
        assertEquals("6231979280641", tracking.trackingNumber().orElseThrow());
        assertTrue(tracking.trackingUrl().isEmpty());
    }

    @Test
    void mapsTheBareUrlShapeOfDeliveryTracking() {
        String json = """
                {"id":"221203x1","status":"pending","items":[],"currency":"PLN","totalPrice":0,
                 "sellerStatus":"created","created":"2026-07-23T10:00:00Z","updated":"2026-07-23T10:00:00Z",
                 "delivery":{"name":"Kurier","typeId":"courier","price":0,"cod":false},
                 "deliveryTracking":{"status":"sent","trackingUrl":"https://track.example.com/abc"}}""";

        DeliveryTracking tracking = OrderMapper
                .toDomain(codec.read(json, io.github.mgrtomaszzurawski.erli.rest.model.Order.class))
                .deliveryTracking().orElseThrow();

        assertEquals(TrackingStatus.SENT, tracking.status());
        assertEquals("https://track.example.com/abc", tracking.trackingUrl().orElseThrow());
        assertTrue(tracking.vendor().isEmpty());
        assertTrue(tracking.trackingNumber().isEmpty());
    }

    @Test
    void mapsRebatePaymentAndReturns() {
        Order order = mapFixture(FULL_FIXTURE);

        Rebate rebate = order.rebate().orElseThrow();
        assertEquals(55L, rebate.id());
        assertEquals("Wiosenna promocja", rebate.name());
        assertEquals("WIOSNA10", rebate.code().orElseThrow());

        assertEquals(778899L, order.payment().orElseThrow().id());
        assertEquals(PaymentStatus.COMPLETED, order.payment().orElseThrow().status().orElseThrow());

        OrderReturn orderReturn = order.returns().get(0);
        assertEquals(ReturnReason.ITEMS_QUALITY, orderReturn.reason());
        assertEquals("Ukruszone ucho", orderReturn.comment().orElseThrow());
        assertEquals(OffsetDateTime.of(2026, 7, 22, 14, 5, 0, 0, ZoneOffset.UTC), orderReturn.created());
        assertEquals("12345678901234567890123456", orderReturn.bankAccount().orElseThrow().number());
        assertEquals("Anna Kowalska", orderReturn.bankAccount().orElseThrow().name());
        assertEquals(0, orderReturn.items().get(0).index());
        // Erli spells the wire property "quentity"; the domain record fixes the spelling.
        assertEquals(1, orderReturn.items().get(0).quantity());
    }

    @Test
    void mapsAPayloadCarryingOnlyTheRequiredFields() {
        Order order = mapFixture(MINIMAL_FIXTURE);

        assertEquals("221202x12346", order.id().value());
        assertEquals(OrderStatus.PENDING, order.status());
        assertEquals(SellerStatus.CREATED, order.sellerStatus());
        assertEquals(new BigDecimal("25.00"), order.totalPrice().amount());
        assertFalse(order.delivery().cashOnDelivery());
        assertTrue(order.buyer().isEmpty());
        assertTrue(order.rebate().isEmpty());
        assertTrue(order.deliveryTracking().isEmpty());
        assertTrue(order.payment().isEmpty());
        assertTrue(order.comment().isEmpty());
        assertTrue(order.externalOrderId().isEmpty());
        assertTrue(order.purchasedAt().isEmpty());
        assertTrue(order.cursor().isEmpty());
        assertTrue(order.calculatedParcelsCount().isEmpty());
        assertTrue(order.delivery().pickupPlace().isEmpty());
        assertTrue(order.delivery().cancelled().isEmpty());
        assertTrue(order.returns().isEmpty());
    }

    @Test
    void rejectsAPayloadMissingARequiredFieldNamingTheField() {
        String missingSellerStatus = """
                {"id":"221204x1","status":"pending","items":[],"currency":"PLN","totalPrice":0,
                 "created":"2026-07-23T10:00:00Z","updated":"2026-07-23T10:00:00Z",
                 "delivery":{"name":"Kurier","typeId":"courier","price":0,"cod":false}}""";

        // Reported inside the documented ErliException taxonomy, so a caller catching ErliException
        // handles a malformed response like any other failure of the call.
        ErliTransportException thrown = assertThrows(ErliTransportException.class, () -> OrderMapper
                .toDomain(codec.read(missingSellerStatus,
                        io.github.mgrtomaszzurawski.erli.rest.model.Order.class)));

        assertInstanceOf(ErliException.class, thrown);
        assertTrue(thrown.getMessage().contains("sellerStatus"),
                "the failure should name the missing field, got: " + thrown.getMessage());
    }

    @Test
    void rebuildsAmountsInEuroToo() {
        // The PLN fixture alone cannot prove the scale comes from the currency, since a hard-coded
        // /100 gives the same answer. EUR is the only other currency Erli sends, and exercising it
        // also covers the second branch of the currency mapping.
        String euroOrder = """
                {"id":"221205x1","status":"purchased","items":[],"currency":"EUR","totalPrice":12787,
                 "sellerStatus":"created","created":"2026-07-23T10:00:00Z","updated":"2026-07-23T10:00:00Z",
                 "delivery":{"name":"Kurier","typeId":"courier","price":1290,"cod":false}}""";

        Order order = OrderMapper.toDomain(
                codec.read(euroOrder, io.github.mgrtomaszzurawski.erli.rest.model.Order.class));

        assertEquals(Currency.getInstance("EUR"), order.totalPrice().currency());
        assertEquals(new BigDecimal("127.87"), order.totalPrice().amount());
        assertEquals(new BigDecimal("12.90"), order.delivery().price().amount());
    }

    /**
     * A carrier newer than this SDK must not sink the order. Since CORE-12 the codec decodes an
     * unrecognised enum to {@code null} rather than throwing, so it reaches the domain as an absent
     * vendor — the rest of the payload still maps. This pins that contract, which is easy to break by
     * making the mapper require the field.
     */
    @Test
    void survivesACarrierThisSdkDoesNotKnow() {
        String futureCarrier = """
                {"id":"221206x1","status":"purchased","items":[],"currency":"PLN","totalPrice":1000,
                 "sellerStatus":"sent","created":"2026-07-23T10:00:00Z","updated":"2026-07-23T10:00:00Z",
                 "delivery":{"name":"Kurier","typeId":"courier","price":0,"cod":false},
                 "deliveryTracking":{"status":"sent","vendor":"carrierAddedNextYear",
                                     "trackingNumber":"TRK-1"}}""";

        Order order = OrderMapper.toDomain(
                codec.read(futureCarrier, io.github.mgrtomaszzurawski.erli.rest.model.Order.class));

        DeliveryTracking tracking = order.deliveryTracking().orElseThrow();
        assertEquals(TrackingStatus.SENT, tracking.status());
        assertEquals("TRK-1", tracking.trackingNumber().orElseThrow());
        // The carrier itself is lost — AS_NULL does not preserve the wire value. Documented in
        // docs/orders.md so an empty vendor is not read as "shipped without a carrier".
        assertTrue(tracking.vendor().isEmpty());
    }

    @Test
    void mapsAKnownCarrierToTheSharedCoreVendorType() {
        DeliveryTracking tracking = mapFixture(FULL_FIXTURE).deliveryTracking().orElseThrow();

        assertEquals(DeliveryVendor.INPOST, tracking.vendor().orElseThrow());
    }

    @Test
    void keepsBuyerFreeTextOutOfStringRenderings() {
        // A buyer types a phone number into the comment box far more often than anyone expects.
        Order order = mapFixture(FULL_FIXTURE);

        assertFalse(order.toString().contains("Prosze zapakowac"),
                "buyer comment leaked into Order.toString(): " + order);
        assertFalse(order.returns().get(0).toString().contains("Ukruszone ucho"),
                "buyer return comment leaked into OrderReturn.toString()");
        assertFalse(order.toString().contains("Fabryczna"),
                "pickup place address leaked into Order.toString(): " + order);
        // Still useful for debugging.
        assertTrue(order.toString().contains("221201x12345"), order.toString());
        assertTrue(order.toString().contains("PURCHASED"), order.toString());
    }

    @Test
    void returnedCollectionsAreImmutable() {
        Order order = mapFixture(FULL_FIXTURE);

        assertThrows(UnsupportedOperationException.class, () -> order.items().clear());
        assertThrows(UnsupportedOperationException.class, () -> order.returns().clear());
    }
}
