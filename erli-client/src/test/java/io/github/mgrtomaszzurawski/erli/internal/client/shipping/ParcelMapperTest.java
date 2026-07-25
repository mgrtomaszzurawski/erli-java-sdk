package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import io.github.mgrtomaszzurawski.erli.domain.shipping.Parcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelError;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelShipment;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatus;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatusChange;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelType;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PickupType;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingCountry;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingParty;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box test of the raw→domain mapping. Asserts the mapped <em>fields</em>, not merely that a
 * call returned without throwing: a facade that maps one field and drops the rest would pass a naive
 * test, and the deep {@code shipping} block is where that would happen.
 */
class ParcelMapperTest {

    private static final String FULL_PARCEL_ID = "55123";
    private static final String FULL_ORDER_ID = "100007x1234";
    private static final String DELIVERY_METHOD = "erliKurier24InPost10kg";
    private static final int VALIDATION_ERROR_CODE = 1201;
    private static final long POSTING_POINT_ID = 4471L;
    private static final int EXPECTED_WEIGHT_GRAMS = 1500;

    private final JsonCodec codec = new JsonCodec();

    private Parcel mapFrom(String json) {
        return ParcelMapper.toDomain(
                codec.read(json, io.github.mgrtomaszzurawski.erli.rest.model.Parcel.class));
    }

    @Test
    void mapsEveryTopLevelFieldOfAFullParcel() {
        Parcel parcel = mapFrom(ParcelFixtures.FULL_PARCEL_JSON);

        assertEquals(Optional.of(ParcelId.of(FULL_PARCEL_ID)), parcel.id());
        assertEquals(ParcelType.INTERNAL, parcel.type());
        assertEquals(Optional.of(OrderId.of(FULL_ORDER_ID)), parcel.orderId());
        assertTrue(parcel.erliPro());
        assertEquals(ParcelStatus.ON_THE_WAY, parcel.status());
        assertEquals(Optional.of("6200000012345"), parcel.trackingNumber());
        assertEquals(OffsetDateTime.parse("2026-07-20T08:14:00Z"), parcel.createdAt());
        assertEquals(OffsetDateTime.parse("2026-07-21T09:30:00Z"), parcel.updatedAt());
    }

    @Test
    void mapsDimensionsPreservingTheScaleTheApiSent() {
        Parcel parcel = mapFrom(ParcelFixtures.FULL_PARCEL_JSON);

        assertEquals(new BigDecimal("20.5"), parcel.dimensions().width());
        assertEquals(new BigDecimal("10"), parcel.dimensions().height());
        assertEquals(new BigDecimal("30.25"), parcel.dimensions().length());
        assertEquals(EXPECTED_WEIGHT_GRAMS, parcel.dimensions().weight());
    }

    @Test
    void mapsErrorsCarriedInsideASuccessfulResponse() {
        Parcel parcel = mapFrom(ParcelFixtures.FULL_PARCEL_JSON);

        assertEquals(1, parcel.errors().size());
        ParcelError error = parcel.errors().get(0);
        assertEquals(VALIDATION_ERROR_CODE, error.errorCode());
        assertEquals(Optional.of("Blad walidacji przesylki"), error.errorMessage());
    }

    @Test
    void mapsStatusHistoryInTheOrderTheApiReturned() {
        Parcel parcel = mapFrom(ParcelFixtures.FULL_PARCEL_JSON);

        List<ParcelStatusChange> history = parcel.statusHistory();
        assertEquals(2, history.size());
        assertEquals(ParcelStatus.PREPARING, history.get(0).status());
        assertEquals(OffsetDateTime.parse("2026-07-20T08:15:00Z"), history.get(0).changed());
        assertEquals(ParcelStatus.SENT, history.get(1).status());
    }

    @Test
    void mapsTheDeepShippingBlockIncludingBothParties() {
        ParcelShipment shipping = mapFrom(ParcelFixtures.FULL_PARCEL_JSON).shipping();

        assertEquals(Optional.of(ShippingMethodId.of(DELIVERY_METHOD)), shipping.deliveryMethod());
        assertEquals(Optional.of(POSTING_POINT_ID), shipping.postingPointId());
        assertEquals(Optional.of("Leave at reception"), shipping.additionalInformation());
        assertFalse(shipping.nonStandard());
        assertEquals(Optional.of(OffsetDateTime.parse("2026-07-21T09:29:00Z")), shipping.registeredAt());
        assertEquals(Optional.of(OffsetDateTime.parse("2026-08-21T09:29:00Z")), shipping.waybillExpiration());
        assertEquals(List.of("https://erli.pl/waybill/55123.pdf"), shipping.waybills());
        assertEquals(Optional.of("https://erli.pl/protocol/55123.pdf"), shipping.pickupProtocol());

        ShippingParty sender = shipping.sender().orElseThrow();
        assertEquals(Optional.of("Test Shop"), sender.companyName());
        assertEquals(Optional.of("Przemyslowa"), sender.street());
        assertEquals(Optional.of("3"), sender.flatNumber());
        assertEquals(Optional.of(PickupType.COURIER), sender.pickupType());

        ShippingParty receiver = shipping.receiver().orElseThrow();
        assertEquals(Optional.of("Jan"), receiver.firstName());
        assertEquals(Optional.of("Nowak"), receiver.lastName());
        assertEquals(Optional.of("Kwiatowa"), receiver.street());
        assertEquals(Optional.of("7"), receiver.buildingNumber());
        assertEquals(Optional.of("Warszawa"), receiver.city());
        assertEquals(Optional.of("00-950"), receiver.zip());
        assertEquals(Optional.of(ShippingCountry.PL), receiver.country());
        assertEquals(Optional.of("600300400"), receiver.phoneNumber());
        assertEquals(Optional.of("buyer@example.test"), receiver.email());
        assertEquals(Optional.of(PickupType.POINT), receiver.pickupType());
        assertEquals(Optional.of("WAW01A"), receiver.pointCode());
        assertTrue(receiver.companyName().isEmpty());
    }

    @Test
    void mapsAMinimalParcelWithEveryOptionalFieldAbsent() {
        Parcel parcel = mapFrom(ParcelFixtures.MINIMAL_PARCEL_JSON);

        assertTrue(parcel.id().isEmpty());
        assertTrue(parcel.orderId().isEmpty());
        assertTrue(parcel.trackingNumber().isEmpty());
        assertFalse(parcel.erliPro());
        assertEquals(List.of(), parcel.errors());
        assertEquals(List.of(), parcel.statusHistory());
        assertEquals(ParcelStatus.PREPARING, parcel.status());
        assertTrue(parcel.shipping().sender().isEmpty());
        assertTrue(parcel.shipping().receiver().isEmpty());
        assertEquals(List.of(), parcel.shipping().waybills());
        assertFalse(parcel.shipping().nonStandard());
    }

    @Test
    void rejectsAResponseMissingASpecRequiredField() {
        String missingStatus = ParcelFixtures.MINIMAL_PARCEL_JSON.replace("\"status\": \"preparing\",", "");

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> mapFrom(missingStatus));

        assertTrue(failure.getMessage().contains("status"), failure.getMessage());
    }

    @Test
    void keepsBuyerPersonalDataOutOfToStringWhileDisclosingPresence() {
        Parcel parcel = mapFrom(ParcelFixtures.FULL_PARCEL_JSON);

        String rendered = parcel.toString();

        assertFalse(rendered.contains("buyer@example.test"), rendered);
        assertFalse(rendered.contains("Kwiatowa"), rendered);
        assertFalse(rendered.contains("600300400"), rendered);
        assertFalse(rendered.contains("Nowak"), rendered);
        assertFalse(rendered.contains("00-950"), rendered);
        // Presence must stay visible, and non-identifying routing detail stays readable.
        assertTrue(rendered.contains("email=***"), rendered);
        assertTrue(rendered.contains("pointCode=WAW01A"), rendered);
    }
}
