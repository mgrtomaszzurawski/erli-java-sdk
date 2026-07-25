package io.github.mgrtomaszzurawski.erli.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.rest.model.Discount;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonCodecTest {

    /**
     * Any generated model carrying OpenAPI {@code date-time} properties serves here; {@code Discount}
     * is one of the smallest. The point is the codec configuration, not the model.
     *
     * <p>Provenance: spec-derived, not captured from the wire. The mixed-offset shape is taken from the
     * vendored {@code openapi/swagger.json}, whose {@code CreateDiscount} example states a non-UTC
     * offset ({@code 2026-07-24T13:50:23.961+02:00}) — so a non-{@code Z} offset is what the API really
     * exchanges, in requests as well as responses.
     */
    private static final String START_AT_PROPERTY = "startAt";

    private static final String MODEL_WITH_TIMESTAMPS_JSON = """
            {
              "externalId": "SKU-1",
              "newPrice": 4999,
              "startAt": "2026-07-20T08:15:00Z",
              "restoreAt": "2026-07-27T08:15:00+02:00"
            }
            """;

    private final JsonCodec codec = new JsonCodec();

    @Test
    void readListDecodesEachElement() {
        String body = "[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]";

        List<ShopResponse> shops = codec.readList(body, ShopResponse.class);

        assertEquals(2, shops.size());
        assertEquals("a", shops.get(0).getName());
        assertEquals(2, shops.get(1).getId().intValue());
    }

    @Test
    void readListDecodesEmptyArray() {
        assertTrue(codec.readList("[]", ShopResponse.class).isEmpty());
    }

    @Test
    void readIgnoresUnknownProperties() {
        ShopResponse shop = codec.read("{\"id\":7,\"name\":\"n\",\"surprise\":true}", ShopResponse.class);
        assertEquals(7, shop.getId().intValue());
    }

    @Test
    void readWrapsMalformedJsonAsTransportError() {
        assertThrows(ErliTransportException.class, () -> codec.read("{not json", ShopResponse.class));
    }

    @Test
    void readTreeLenientReturnsNullForNonJson() {
        assertNull(codec.readTreeLenient("<html>nope</html>"));
        assertNull(codec.readTreeLenient(""));
    }

    @Test
    void unknownEnumValueDecodesToNullInsteadOfFailingTheWholeResponse() {
        // CORE-12: a wire value the vendored spec doesn't know (a new carrier here) must not throw and
        // sink the entire response — the strict generated enum creator would. The field comes back null;
        // the domain layer decides whether that is fail-loud (required) or an UNRECOGNIZED sentinel.
        DeliveryMethod method = codec.read(
                "{\"id\":\"x\",\"name\":\"n\",\"cod\":true,\"vendor\":\"quantumTeleport\"}", DeliveryMethod.class);

        assertNull(method.getVendor(), "unknown enum should decode to null");
        assertEquals("x", method.getId());
    }

    // Every OpenAPI `date-time` property becomes an OffsetDateTime in the generated Layer-1 models, and
    // a bare ObjectMapper rejects those outright. ShopResponse — the only payload the shop slice decodes
    // — happens to carry no timestamp, so nothing exercised this until a domain bucket decoded a real
    // one. The tests below pin the configuration on both the read and the write side.

    @Test
    void readDecodesGeneratedModelsThatCarryDateTimeProperties() {
        Discount discount = codec.read(MODEL_WITH_TIMESTAMPS_JSON, Discount.class);

        assertEquals(OffsetDateTime.parse("2026-07-20T08:15:00Z"), discount.getStartAt());
    }

    @Test
    void readPreservesTheOffsetTheApiSentInsteadOfRewritingItToUtc() {
        Discount discount = codec.read(MODEL_WITH_TIMESTAMPS_JSON, Discount.class);

        // The offset is the assertion that matters: normalizing to UTC would keep the same instant but
        // report an offset the server never stated.
        assertNotNull(discount.getRestoreAt(), "restoreAt did not decode at all");
        assertEquals(ZoneOffset.ofHours(2), discount.getRestoreAt().getOffset());
        assertTrue(discount.getRestoreAt().isEqual(OffsetDateTime.parse("2026-07-27T06:15:00Z")));
    }

    @Test
    void writeEmitsDatesAsJsonStringsRatherThanEpochNumbers() {
        Discount discount = new Discount();
        discount.setStartAt(OffsetDateTime.parse("2026-07-20T08:15:00Z"));

        JsonNode written = codec.readTreeLenient(codec.write(discount));

        // Asserting on the parsed node, not a substring: the regression guarded here is a date leaving
        // as a number, which a `contains` check on formatted text would not distinguish reliably. The
        // presence check first, so a vanished property names itself instead of surfacing as an NPE.
        assertTrue(written != null && written.has(START_AT_PROPERTY), "written JSON: " + written);
        assertTrue(written.get(START_AT_PROPERTY).isTextual(), written.toString());
        assertEquals("2026-07-20T08:15:00Z", written.get(START_AT_PROPERTY).asText());
    }

    @Test
    void writeEmitsTheOffsetTheValueCarriesRatherThanConvertingItToUtc() {
        Discount discount = new Discount();
        discount.setStartAt(OffsetDateTime.parse("2026-07-27T08:15:00+02:00"));

        JsonNode written = codec.readTreeLenient(codec.write(discount));

        assertTrue(written != null && written.has(START_AT_PROPERTY), "written JSON: " + written);
        assertEquals("2026-07-27T08:15:00+02:00", written.get(START_AT_PROPERTY).asText(), written.toString());
    }
}
