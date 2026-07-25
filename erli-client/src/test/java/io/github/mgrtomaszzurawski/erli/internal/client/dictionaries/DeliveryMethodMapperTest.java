package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryMethodMapperTest {

    /**
     * Verbatim from the live sandbox: {@code GET /dictionaries/deliveryMethods} on
     * {@code https://sandbox.erli.dev/svc/shop-api}, 2026-07-25 (first two of 114 entries).
     */
    private static final String OBSERVED_DELIVERY_METHODS_JSON = """
            [
              {"id":"erliPaczkomat","name":"ERLI InPost Paczkomaty 24/7","cod":false,"vendor":"inpost"},
              {"id":"kurierPobranie","name":"Kurier za pobraniem","cod":true,"vendor":"dpd"}
            ]""";

    private static final String MISSING_VENDOR_JSON =
            "[{\"id\":\"erliPaczkomat\",\"name\":\"ERLI InPost Paczkomaty 24/7\",\"cod\":false}]";

    private static io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[] decode(String json) {
        return new JsonCodec().read(json, io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[].class);
    }

    @Test
    void mapsEveryFieldOfTheObservedPayload() {
        List<DeliveryMethod> methods = DeliveryMethodMapper.toDomainList(decode(OBSERVED_DELIVERY_METHODS_JSON));

        assertEquals(2, methods.size());

        DeliveryMethod paczkomat = methods.get(0);
        assertEquals(DeliveryMethodId.of("erliPaczkomat"), paczkomat.id());
        assertEquals("ERLI InPost Paczkomaty 24/7", paczkomat.name());
        assertFalse(paczkomat.cashOnDelivery());
        assertEquals(DeliveryVendor.INPOST, paczkomat.vendor());

        DeliveryMethod courier = methods.get(1);
        assertEquals(DeliveryMethodId.of("kurierPobranie"), courier.id());
        assertEquals("Kurier za pobraniem", courier.name());
        assertTrue(courier.cashOnDelivery());
        assertEquals(DeliveryVendor.DPD, courier.vendor());
    }

    @Test
    void mapsAnEmptyArrayToAnEmptyList() {
        assertTrue(DeliveryMethodMapper.toDomainList(decode("[]")).isEmpty());
    }

    @Test
    void mapsANullArrayToAnEmptyList() {
        assertTrue(DeliveryMethodMapper.toDomainList(null).isEmpty());
    }

    @Test
    void rejectsAPayloadMissingASpecRequiredField() {
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[] raw = decode(MISSING_VENDOR_JSON);

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> DeliveryMethodMapper.toDomainList(raw));

        assertTrue(failure.getMessage().contains("vendor"), failure.getMessage());
    }

    @Test
    void carriesAnUnrecognisedCarrierThroughByValue() {
        // The carrier list is server-owned reference data; a carrier this SDK version does not name
        // must still reach the caller. Constructed directly because Layer 1 decodes vendor as an enum.
        DeliveryVendor futureCarrier = DeliveryVendor.of("someFutureCarrier");

        assertEquals("someFutureCarrier", futureCarrier.value());
    }
}
