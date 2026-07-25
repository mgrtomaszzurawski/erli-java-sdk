package io.github.mgrtomaszzurawski.erli.internal.client.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Buyer;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Country;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Delivery;
import io.github.mgrtomaszzurawski.erli.domain.inbox.DeliveryAddress;
import io.github.mgrtomaszzurawski.erli.domain.inbox.DeliveryTracking;
import io.github.mgrtomaszzurawski.erli.domain.inbox.InvoiceAddress;
import io.github.mgrtomaszzurawski.erli.domain.inbox.InvoiceAddressType;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Message;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageId;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageQuery;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageType;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderEvent;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderLine;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderReturn;
import io.github.mgrtomaszzurawski.erli.domain.inbox.OrderStatus;
import io.github.mgrtomaszzurawski.erli.domain.inbox.PaymentStatus;
import io.github.mgrtomaszzurawski.erli.domain.inbox.PickupPlace;
import io.github.mgrtomaszzurawski.erli.domain.inbox.PickupProvider;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ProductsSyncEvent;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ReadReceipt;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Rebate;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ReturnReason;
import io.github.mgrtomaszzurawski.erli.domain.inbox.SellerStatus;
import io.github.mgrtomaszzurawski.erli.domain.inbox.TaxRate;
import io.github.mgrtomaszzurawski.erli.domain.inbox.TrackingStatus;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deep field-level assertions on the inbox mapping. The two fixtures are derived from the vendored
 * spec's message schemas (the sandbox shop is empty, so no observed message exists yet — see
 * {@code KNOWN-SERVER-BEHAVIORS.md}); each nested object is populated so a dropped field fails here.
 */
class MessageMapperTest {

    private static final String ORDER_FIXTURE = "/fixtures/inbox/order-created-message.json";
    private static final String TRACKING_URL = "https://inpost.pl/sledzenie-przesylek?number=628012345678";
    private static final String SYNC_FIXTURE = "/fixtures/inbox/products-need-sync-message.json";
    private static final String OBSERVED_SYNC_FIXTURE =
            "/fixtures/inbox/observed-products-need-sync-message.json";

    private static final long EXPECTED_SHOP_ID = 100007L;
    private static final long EXPECTED_LINE_ID = 9001L;
    private static final int EXPECTED_QUANTITY = 2;
    private static final int EXPECTED_PARCELS = 1;
    private static final long EXPECTED_PAYMENT_ID = 7788L;
    private static final long EXPECTED_REBATE_ID = 55L;
    private static final long EXPECTED_PICKUP_ID = 4242L;
    private static final int EXPECTED_RETURN_QUANTITY = 1;

    private final JsonCodec codec = new JsonCodec();

    @Test
    void mapsEveryEnvelopeFieldOfAnOrderMessage() {
        Message message = mapFixture(ORDER_FIXTURE);

        assertEquals(MessageId.of("5f9e1b3b0f0b9b0001c3e0a0"), message.id());
        assertEquals(EXPECTED_SHOP_ID, message.shopId());
        assertEquals(OffsetDateTime.parse("2026-07-25T10:15:30+02:00"), message.created());
        assertFalse(message.read());
        assertEquals(MessageType.ORDER_CREATED, message.type());
        assertEquals("orderCreated", message.typeName());
        assertTrue(message.orderEvent().isPresent());
    }

    @Test
    void mapsTheOrderSnapshotIncludingMoneyAndTimestamps() {
        OrderEvent order = mapFixture(ORDER_FIXTURE).orderEvent().orElseThrow();

        assertEquals("ORD-1", order.id().value());
        assertEquals("SHOP-77", order.externalOrderId().orElseThrow());
        assertEquals(OrderStatus.PURCHASED, order.status());
        assertEquals(SellerStatus.READY_TO_PROCESS, order.sellerStatus());
        assertEquals("Prosze zapakowac na prezent", order.comment().orElseThrow());
        assertEquals(EXPECTED_PARCELS, order.calculatedParcelsCount().orElseThrow());
        assertEquals("PLN", order.currency().getCurrencyCode());
        // Prices arrive as minor units: 11297 grosze is 112.97 PLN.
        assertEquals(new BigDecimal("112.97"), order.totalPrice().amount());
        assertEquals(OffsetDateTime.parse("2026-07-25T10:15:00+02:00"), order.created());
        assertEquals(OffsetDateTime.parse("2026-07-25T11:00:00+02:00"), order.updated());
        assertEquals(OffsetDateTime.parse("2026-07-25T10:16:00+02:00"), order.purchasedAt().orElseThrow());
        assertEquals("1774432500;id9001", order.cursor().orElseThrow().value());
    }

    @Test
    void mapsBuyerAndBothAddresses() {
        Buyer buyer = mapFixture(ORDER_FIXTURE).orderEvent().orElseThrow().buyer().orElseThrow();

        assertEquals("buyer@example.com", buyer.email());
        DeliveryAddress delivery = buyer.deliveryAddress();
        assertEquals("Anna", delivery.firstName());
        assertEquals("Kowalska", delivery.lastName());
        assertEquals("Kowalska Sp. z o.o.", delivery.companyName().orElseThrow());
        assertEquals("Prosta 12/3", delivery.address());
        assertEquals("Prosta", delivery.street());
        assertEquals("12", delivery.buildingNumber());
        assertEquals("3", delivery.flatNumber().orElseThrow());
        assertEquals("00-838", delivery.zip());
        assertEquals("Warszawa", delivery.city());
        assertEquals(Country.PL, delivery.country());
        assertEquals("601234567", delivery.phone());

        InvoiceAddress invoice = buyer.invoiceAddress().orElseThrow();
        assertEquals(InvoiceAddressType.COMPANY, invoice.type());
        assertEquals("Krucza 5", invoice.address());
        assertEquals("Krucza", invoice.street());
        assertEquals("5", invoice.buildingNumber());
        assertEquals("10", invoice.flatNumber().orElseThrow());
        assertEquals("00-548", invoice.zip());
        assertEquals("Warszawa", invoice.city());
        assertEquals(Country.PL, invoice.country());
        assertEquals("Anna", invoice.firstName().orElseThrow());
        assertEquals("Kowalska", invoice.lastName().orElseThrow());
        assertEquals("Kowalska Sp. z o.o.", invoice.companyName().orElseThrow());
        assertEquals("5252445767", invoice.nip().orElseThrow());
    }

    @Test
    void mapsOrderLinesRebateAndDelivery() {
        OrderEvent order = mapFixture(ORDER_FIXTURE).orderEvent().orElseThrow();

        OrderLine line = order.lines().get(0);
        assertEquals(EXPECTED_LINE_ID, line.id());
        assertEquals(ProductExternalId.of("SKU-1"), line.externalId());
        assertEquals(EXPECTED_QUANTITY, line.quantity());
        assertEquals(new BigDecimal("1.25"), line.weight().orElseThrow());
        assertEquals(new BigDecimal("49.99"), line.unitPrice().amount());
        assertEquals(new BigDecimal("59.99"), line.unitPriceBeforeRebate().orElseThrow().amount());
        assertEquals("Kubek termiczny", line.name());
        assertEquals("kubek-termiczny", line.slug());
        assertEquals("5901234123457", line.ean().orElseThrow());
        assertEquals("KT-500", line.sku().orElseThrow());
        assertEquals(TaxRate.TAX_23, line.taxRate().orElseThrow());

        Rebate rebate = order.rebate().orElseThrow();
        assertEquals(EXPECTED_REBATE_ID, rebate.id());
        assertEquals("Lato 2026", rebate.name());
        assertEquals("LATO10", rebate.code().orElseThrow());

        Delivery delivery = order.delivery();
        assertEquals("Paczkomat InPost", delivery.name());
        assertEquals("inpost-locker", delivery.typeId().value());
        assertEquals(new BigDecimal("12.99"), delivery.price().amount());
        assertEquals(new BigDecimal("0.00"), delivery.cancelledPrice().orElseThrow().amount());
        assertFalse(delivery.cashOnDelivery());
        assertEquals("pl", delivery.sourceMarket().orElseThrow());
        assertEquals("pl", delivery.targetMarket().orElseThrow());

        PickupPlace pickup = delivery.pickupPlace().orElseThrow();
        assertEquals(EXPECTED_PICKUP_ID, pickup.id().orElseThrow());
        assertEquals("WAW01A", pickup.externalId().orElseThrow());
        assertEquals("Paczkomat WAW01A", pickup.heading().orElseThrow());
        assertEquals("locker", pickup.type().orElseThrow());
        assertEquals(PickupProvider.INPOST, pickup.provider().orElseThrow());
        assertEquals("WAW01A", pickup.name().orElseThrow());
        assertEquals("Przy wejsciu do sklepu", pickup.description().orElseThrow());
        assertEquals("Prosta 51", pickup.address().orElseThrow());
        assertEquals("Warszawa", pickup.city().orElseThrow());
        assertEquals("pl", pickup.country().orElseThrow());
        assertTrue(pickup.open24h().orElseThrow());
        assertEquals("00-838", pickup.zip().orElseThrow());
    }

    /**
     * The carrier branch of {@code deliveryTracking} is the one the generated {@code anyOf} wrapper
     * would drop, because the URL branch is declared first and matches leniently. Asserting
     * {@code vendor} and {@code trackingNumber} is what keeps that regression out.
     */
    @Test
    void mapsTheCarrierBranchOfDeliveryTracking() {
        DeliveryTracking tracking =
                mapFixture(ORDER_FIXTURE).orderEvent().orElseThrow().deliveryTracking().orElseThrow();

        assertEquals(TrackingStatus.SENT, tracking.status());
        assertEquals(DeliveryVendor.INPOST, tracking.vendor().orElseThrow());
        assertEquals("628012345678", tracking.trackingNumber().orElseThrow());
        assertTrue(tracking.trackingUrl().isEmpty());
    }

    /**
     * The other declared shape of {@code deliveryTracking}: a bare tracking URL, with no carrier or
     * consignment number.
     *
     * <p>Added when the Orders bucket's Layer-1 composite merge (CORE-3) replaced this mapper's
     * two-branch discrimination with a single bind. Only the carrier shape was covered before, so the
     * URL shape was the branch the rewrite could have broken silently — the whole point of the merge
     * being that both shapes now arrive through one object.
     */
    @Test
    void mapsTheUrlBranchOfDeliveryTracking() {
        String urlShaped = withTrackingBlock(
                "\"status\": \"sent\", \"trackingUrl\": \"" + TRACKING_URL + "\"");

        DeliveryTracking tracking = MessageMapper
                .toDomain(codec.readTreeLenient(urlShaped), codec)
                .orderEvent().orElseThrow()
                .deliveryTracking().orElseThrow();

        assertEquals(TrackingStatus.SENT, tracking.status());
        assertEquals(TRACKING_URL, tracking.trackingUrl().orElseThrow());
        assertTrue(tracking.vendor().isEmpty());
        assertTrue(tracking.trackingNumber().isEmpty());
    }

    /**
     * {@code status} is the one property both tracking shapes require, so it is what the single bind
     * has to keep validating now that the branch classes are gone.
     */
    @Test
    void rejectsDeliveryTrackingWithoutAStatus() {
        String withoutStatus = withTrackingBlock("\"trackingUrl\": \"" + TRACKING_URL + "\"");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> MessageMapper.toDomain(codec.readTreeLenient(withoutStatus), codec));

        // The type alone does not discriminate: every missing required field in this mapper throws
        // IllegalStateException, so assert the message names the check that actually fired.
        assertTrue(thrown.getMessage().contains("status"),
                "the failure should name the missing field, got: " + thrown.getMessage());
    }

    /**
     * Swap the fixture's whole {@code deliveryTracking} object for {@code properties}. Replacing the
     * block rather than individual lines keeps the JSON valid whichever way the fixture is punctuated.
     */
    private static String withTrackingBlock(String properties) {
        String replaced = readFixture(ORDER_FIXTURE)
                .replaceAll("(?s)\"deliveryTracking\"\\s*:\\s*\\{.*?}", "\"deliveryTracking\": {" + properties + "}");
        assertNotEquals(readFixture(ORDER_FIXTURE), replaced, "fixture no longer has a deliveryTracking block");
        return replaced;
    }

    @Test
    void mapsPaymentAndReturns() {
        OrderEvent order = mapFixture(ORDER_FIXTURE).orderEvent().orElseThrow();

        assertEquals(EXPECTED_PAYMENT_ID, order.payment().orElseThrow().id());
        assertEquals(PaymentStatus.COMPLETED, order.payment().orElseThrow().status().orElseThrow());

        OrderReturn orderReturn = order.returns().get(0);
        assertEquals(0, orderReturn.lines().get(0).lineIndex());
        // The spec spells the property 'quentity'; the domain exposes the correct English word.
        assertEquals(EXPECTED_RETURN_QUANTITY, orderReturn.lines().get(0).quantity());
        assertEquals("12345678901234567890123456", orderReturn.bankAccount().orElseThrow().number());
        assertEquals("Anna Kowalska", orderReturn.bankAccount().orElseThrow().name());
        assertEquals(ReturnReason.DOES_NOT_FIT, orderReturn.reason());
        assertEquals("Za maly", orderReturn.comment().orElseThrow());
        assertEquals(OffsetDateTime.parse("2026-07-26T08:00:00+02:00"), orderReturn.created());
    }

    /**
     * The regression that motivates the tree-based mapping: a {@code productsNeedSync} payload matches
     * the order branch of the {@code anyOf} leniently (every order field simply lands as null), so a
     * wrapper-driven mapping would return an order event with no data and lose the product ids. The
     * payload branch must be chosen by the message {@code type}.
     */
    @Test
    void mapsProductSyncPayloadRatherThanBindingTheFirstAnyOfBranch() {
        Message message = mapFixture(SYNC_FIXTURE);

        assertEquals(MessageType.PRODUCTS_NEED_SYNC, message.type());
        assertTrue(message.orderEvent().isEmpty(), "a sync message must not map to an order event");
        ProductsSyncEvent sync = message.productsSyncEvent().orElseThrow();
        assertEquals("d41d8cd98f00b204e9800998", sync.id().orElseThrow());
        assertEquals(
                List.of(ProductExternalId.of("SKU-1"), ProductExternalId.of("SKU-2")),
                sync.productIds());
        assertEquals(List.of("price", "stock"), sync.fields());
        assertFalse(sync.isWholeProduct());
    }

    /**
     * The message actually observed on the sandbox on 2026-07-25, raised by firing
     * {@code POST /hooks/productsNeedSync/run}. Its payload carries <strong>neither</strong> the
     * {@code id} the spec marks required <strong>nor</strong> {@code fields} — a spec-vs-server
     * divergence that a spec-derived fixture alone would never have caught.
     */
    @Test
    void mapsTheProductSyncMessageObservedLiveWhosePayloadOmitsTheRequiredId() {
        JsonNode observed = codec.readTreeLenient(readFixture(OBSERVED_SYNC_FIXTURE)).get(0);

        Message message = MessageMapper.toDomain(observed, codec);

        assertEquals(MessageId.of("6a64b7539355872fb8f579c5"), message.id());
        assertEquals(EXPECTED_SHOP_ID, message.shopId());
        assertEquals(OffsetDateTime.parse("2026-07-25T13:17:07.175Z"), message.created());
        assertFalse(message.read());
        ProductsSyncEvent sync = message.productsSyncEvent().orElseThrow();
        assertTrue(sync.id().isEmpty(), "the observed payload carries no id, and that must not throw");
        assertEquals(List.of(ProductExternalId.of("probe-1")), sync.productIds());
        assertEquals(List.of(), sync.fields());
        assertTrue(sync.isWholeProduct());
    }

    @Test
    void treatsAnUnknownTypeAsUnknownWithoutGuessingThePayload() {
        String unknownType = readFixture(SYNC_FIXTURE).replace("\"productsNeedSync\"", "\"somethingNew\"");

        Message message = MessageMapper.toDomain(codec.readTreeLenient(unknownType), codec);

        assertEquals(MessageType.UNKNOWN, message.type());
        assertEquals("somethingNew", message.typeName());
        assertTrue(message.payload().isEmpty(), "an unknown type must not be mapped to a guessed payload");
    }

    @Test
    void rejectsAMessageMissingARequiredField() {
        String withoutShopId = readFixture(SYNC_FIXTURE).replace("\"shopId\": 100007,", "");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> MessageMapper.toDomain(codec.readTreeLenient(withoutShopId), codec));
        assertTrue(thrown.getMessage().contains("shopId"), thrown.getMessage());
    }

    @Test
    void buildsTheSearchRequestOnlyWhenTypesAreRequested() {
        assertNull(MessageMapper.toRaw(MessageQuery.all()).getTypes(),
                "an unfiltered query must leave 'types' absent, not send an empty array");
        assertEquals(
                List.of("orderCreated", "productsNeedSync"),
                MessageMapper.toRaw(MessageQuery.ofTypes(
                        Set.of(MessageType.PRODUCTS_NEED_SYNC, MessageType.ORDER_CREATED))).getTypes());
    }

    @Test
    void buildsBothMarkReadShapes() {
        Object upTo = MessageMapper.toRaw(ReadReceipt.upTo(MessageId.of("5f9e1b3b0f0b9b0001c3e0a0")))
                .getActualInstance();
        Object exactly = MessageMapper.toRaw(ReadReceipt.exactly(List.of(
                MessageId.of("5f9e1b3b0f0b9b0001c3e0a0"), MessageId.of("5f9e1b3b0f0b9b0001c3e0b1"))))
                .getActualInstance();

        assertEquals("{\"lastMessageId\":\"5f9e1b3b0f0b9b0001c3e0a0\"}", codec.write(upTo));
        assertEquals(
                "{\"ids\":[\"5f9e1b3b0f0b9b0001c3e0a0\",\"5f9e1b3b0f0b9b0001c3e0b1\"]}",
                codec.write(exactly));
    }

    private Message mapFixture(String resource) {
        JsonNode node = codec.readTreeLenient(readFixture(resource));
        return MessageMapper.toDomain(node, codec);
    }

    private static String readFixture(String resource) {
        try (InputStream stream = MessageMapperTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test fixture " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read test fixture " + resource, failure);
        }
    }
}
