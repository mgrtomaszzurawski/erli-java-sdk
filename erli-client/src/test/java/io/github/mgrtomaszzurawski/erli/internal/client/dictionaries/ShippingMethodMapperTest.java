package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ParcelDimensions;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingOperator;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShippingMethodMapperTest {

    /**
     * Verbatim from the live sandbox: {@code GET /dictionaries/shippingMethods}, 2026-07-25. The three
     * entries cover both shapes the {@code maxDimensions} anyOf takes in production — explicit box
     * dimensions, and the longest-side/dimensions-sum form used for oversized parcels.
     */
    private static final String OBSERVED_SHIPPING_METHODS_JSON = """
            [
              {"id":"erliPaczkomat","name":"ERLI InPost Paczkomaty 24/7","groupId":"erliPaczkomat",
               "operator":"INPOST","cod":false,"maxUnitPrice":1049,
               "maxDimensions":{"height":64,"width":38,"length":41,"weight":25000}},
              {"id":"erliDPDKurier500kg","name":"ERLI DPD Kurier","groupId":"erliDPDKurier",
               "operator":"DPD","cod":true,
               "maxDimensions":{"longestSide":300,"dimensionsSum":600,"weight":500000,
                                "withVolumetricScales":true}},
              {"id":"erliPocztexKurierS","name":"ERLI Pocztex Kurier S","operator":"POCZTA","cod":false,
               "minDimensions":{"height":1,"width":9,"length":14,"weight":1},
               "maxPointDimensions":{"height":20,"width":30,"length":40,"weight":5000}}
            ]""";

    private static final JsonCodec CODEC = new JsonCodec();

    private static List<ShippingMethod> mapAll(String json) {
        JsonNode[] rawMethods = CODEC.read(json, JsonNode[].class);
        return Arrays.stream(rawMethods).map(rawMethod -> ShippingMethodMapper.toDomain(rawMethod, CODEC)).toList();
    }

    @Test
    void mapsTheBoxFormOfTheDimensionsAnyOf() {
        ShippingMethod method = mapAll(OBSERVED_SHIPPING_METHODS_JSON).get(0);

        assertEquals(ShippingMethodId.of("erliPaczkomat"), method.id());
        assertEquals("ERLI InPost Paczkomaty 24/7", method.name());
        assertEquals("erliPaczkomat", method.groupId().orElseThrow());
        assertEquals(ShippingOperator.INPOST, method.operator().orElseThrow());
        assertFalse(method.cashOnDelivery());
        assertEquals(1049, method.maxUnitPrice().orElseThrow());

        ParcelDimensions bound = method.maxDimensions().orElseThrow();
        ParcelDimensions.Box box = assertInstanceOf(ParcelDimensions.Box.class, bound);
        assertEquals(new BigDecimal("64"), box.height().orElseThrow());
        assertEquals(new BigDecimal("38"), box.width().orElseThrow());
        assertEquals(new BigDecimal("41"), box.length().orElseThrow());
        assertEquals(25000, box.weight().orElseThrow());
        assertTrue(box.withVolumetricScales().isEmpty());
    }

    @Test
    void mapsTheGirthFormOfTheDimensionsAnyOf() {
        ShippingMethod method = mapAll(OBSERVED_SHIPPING_METHODS_JSON).get(1);

        assertEquals(ShippingOperator.DPD, method.operator().orElseThrow());
        assertTrue(method.cashOnDelivery());
        assertTrue(method.maxUnitPrice().isEmpty());

        ParcelDimensions bound = method.maxDimensions().orElseThrow();
        ParcelDimensions.Girth girth = assertInstanceOf(ParcelDimensions.Girth.class, bound);
        assertEquals(new BigDecimal("300"), girth.longestSide().orElseThrow());
        assertEquals(new BigDecimal("600"), girth.dimensionsSum().orElseThrow());
        assertEquals(500000, girth.weight().orElseThrow());
        assertTrue(girth.withVolumetricScales().orElseThrow());
    }



    @Test
    void mapsTheMinimumAndPickupPointBoundsAndOmitsAbsentOnes() {
        ShippingMethod method = mapAll(OBSERVED_SHIPPING_METHODS_JSON).get(2);

        assertTrue(method.groupId().isEmpty());
        assertTrue(method.maxDimensions().isEmpty());

        ParcelDimensions.Box min = assertInstanceOf(ParcelDimensions.Box.class, method.minDimensions().orElseThrow());
        assertEquals(new BigDecimal("14"), min.length().orElseThrow());

        ParcelDimensions.Box point =
                assertInstanceOf(ParcelDimensions.Box.class, method.maxPointDimensions().orElseThrow());
        assertEquals(5000, point.weight().orElseThrow());
    }

    /**
     * The reason the mapper discriminates on the raw tree: bound straight to the box branch, a girth
     * payload parses happily and keeps none of its fields. This is what {@code toBound} avoids.
     */
    @Test
    void theBoxBranchStillSwallowsAGirthPayloadIfBoundDirectly() {
        io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethodMaxDimensionsAnyOf boxBranch =
                CODEC.read("{\"longestSide\":300,\"dimensionsSum\":600,\"weight\":500000}",
                        io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethodMaxDimensionsAnyOf.class);

        assertNull(boxBranch.getHeight());
        assertNull(boxBranch.getWidth());
        assertNull(boxBranch.getLength());
    }

    @Test
    void reportsAnAbsentBoundAsEmpty() {
        ShippingMethod method = mapAll(OBSERVED_SHIPPING_METHODS_JSON).get(2);

        assertTrue(method.maxDimensions().isEmpty());
    }

    @Test
    void mapsAnEmptyPayloadToAnEmptyList() {
        assertTrue(mapAll("[]").isEmpty());
    }

    @Test
    void keepsEveryEntryInTheOrderTheApiReturnedThem() {
        List<ShippingMethod> methods = mapAll(OBSERVED_SHIPPING_METHODS_JSON);

        assertEquals(
                List.of(ShippingMethodId.of("erliPaczkomat"), ShippingMethodId.of("erliDPDKurier500kg"),
                        ShippingMethodId.of("erliPocztexKurierS")),
                methods.stream().map(ShippingMethod::id).toList());
    }
}
