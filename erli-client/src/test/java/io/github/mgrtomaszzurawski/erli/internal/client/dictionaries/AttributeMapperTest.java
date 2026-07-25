package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.AttributeId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Attribute;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttributeType;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttributeValues;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.AttributeValuesResponseInner;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeMapperTest {

    /**
     * Verbatim from the live sandbox: {@code POST /dictionaries/attributes/_search} with
     * {@code {"categoryId":4}}, 2026-07-25. Note {@code slug}, which the API returns but the published
     * schema does not declare — it is dropped by the ignore-unknown codec (see KNOWN-SERVER-BEHAVIORS).
     */
    private static final String OBSERVED_ATTRIBUTES_JSON = """
            [
              {"id":932,"slug":"typPokrycia","name":"Typ pokrycia","required":false,"variantable":true,
               "sameValueForcedOnVariants":false,"type":"dictionary","maxValues":99},
              {"id":99,"slug":"stan","name":"Stan","required":true,"variantable":false,
               "sameValueForcedOnVariants":true,"type":"dictionary","maxValues":1},
              {"id":7529,"slug":"wagaZOpakowaniem","name":"Waga (z opakowaniem)","required":false,
               "variantable":false,"sameValueForcedOnVariants":false,"type":"number","maxValues":1,
               "min":0.001,"max":1000,"precision":3,"unit":"kg"}
            ]""";

    /**
     * Verbatim from the live sandbox: {@code POST /dictionaries/attributeValues/_search} with
     * {@code {"categoryId":4}}, 2026-07-25. The API returns {@code valueIds} as JSON numbers although
     * the schema declares strings; Jackson coerces them, which this fixture pins.
     */
    private static final String OBSERVED_ATTRIBUTE_VALUES_JSON = """
            [{"id":932,
              "values":["Blacha trapezowa","Blachodachówka","Papa"],
              "valueIds":[15594,15595,15599]}]""";

    private static AttributeResponseInner[] decodeAttributes(String json) {
        return new JsonCodec().read(json, AttributeResponseInner[].class);
    }

    private static AttributeValuesResponseInner[] decodeValues(String json) {
        return new JsonCodec().read(json, AttributeValuesResponseInner[].class);
    }

    private static List<Attribute> mapAttributes(String json) {
        return Arrays.stream(decodeAttributes(json)).map(AttributeMapper::toAttribute).toList();
    }

    private static List<AttributeValues> mapAttributeValues(String json) {
        return Arrays.stream(decodeValues(json)).map(AttributeMapper::toAttributeValues).toList();
    }

    @Test
    void mapsADictionaryAttributeWithItsFlags() {
        Attribute attribute = mapAttributes(OBSERVED_ATTRIBUTES_JSON).get(0);

        assertEquals(AttributeId.of("932"), attribute.id());
        assertEquals("Typ pokrycia", attribute.name());
        assertFalse(attribute.required());
        assertTrue(attribute.variantable());
        assertFalse(attribute.sameValueForcedOnVariants());
        assertEquals(AttributeType.DICTIONARY, attribute.type());
        assertEquals(new BigDecimal("99"), attribute.maxValues());
        assertTrue(attribute.unit().isEmpty());
    }

    @Test
    void mapsARequiredAttribute() {
        Attribute attribute = mapAttributes(OBSERVED_ATTRIBUTES_JSON).get(1);

        assertTrue(attribute.required());
        assertTrue(attribute.sameValueForcedOnVariants());
    }

    @Test
    void mapsTheNumericBoundsAndUnitOfANumberAttribute() {
        Attribute attribute = mapAttributes(OBSERVED_ATTRIBUTES_JSON).get(2);

        assertEquals(AttributeType.NUMBER, attribute.type());
        assertEquals(new BigDecimal("0.001"), attribute.min().orElseThrow());
        assertEquals(new BigDecimal("1000"), attribute.max().orElseThrow());
        assertEquals(new BigDecimal("3"), attribute.precision().orElseThrow());
        assertEquals("kg", attribute.unit().orElseThrow());
    }

    @Test
    void treatsAnAbsentBooleanFlagAsFalseRatherThanFailing() {
        // The spec marks the three flags required, but the live API omits them on some attributes.
        AttributeResponseInner[] raw =
                decodeAttributes("[{\"id\":1,\"name\":\"Bez flag\",\"type\":\"string\",\"maxValues\":1}]");

        Attribute attribute = AttributeMapper.toAttribute(raw[0]);

        assertFalse(attribute.required());
        assertFalse(attribute.variantable());
        assertFalse(attribute.sameValueForcedOnVariants());
        assertEquals(AttributeType.STRING, attribute.type());
    }

    @Test
    void rejectsAnAttributeMissingASpecRequiredField() {
        AttributeResponseInner[] raw = decodeAttributes("[{\"id\":1,\"name\":\"Bez typu\",\"maxValues\":1}]");

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> AttributeMapper.toAttribute(raw[0]));

        assertTrue(failure.getMessage().contains("type"), failure.getMessage());
    }

    @Test
    void mapsAttributeValuesAndKeepsLabelsAlignedWithIds() {
        List<AttributeValues> values =
                mapAttributeValues(OBSERVED_ATTRIBUTE_VALUES_JSON);

        assertEquals(1, values.size());
        AttributeValues first = values.get(0);
        assertEquals(AttributeId.of("932"), first.attributeId());
        assertEquals(List.of("Blacha trapezowa", "Blachodachówka", "Papa"), first.values());
        // Numbers on the wire, strings in the schema — pinned so a codec change cannot silently break it.
        assertEquals(List.of("15594", "15595", "15599"), first.valueIds());
        assertEquals(3, first.pairCount());
        assertEquals(List.of("15595", "Blachodachówka"), first.valueAt(1));
    }

    @Test
    void boundsPairAccessByTheShorterOfTheTwoLists() {
        AttributeValuesResponseInner[] raw =
                decodeValues("[{\"id\":1,\"values\":[\"a\",\"b\"],\"valueIds\":[10]}]");

        AttributeValues values = AttributeMapper.toAttributeValues(raw[0]);

        assertEquals(1, values.pairCount());
        assertThrows(IndexOutOfBoundsException.class, () -> values.valueAt(1));
    }

    @Test
    void mapsAnEmptyPayloadToAnEmptyList() {
        assertTrue(mapAttributes("[]").isEmpty());
        assertTrue(mapAttributeValues("[]").isEmpty());
    }
}
