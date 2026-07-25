package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.rest.model.Discount;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponse;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonCodecTest {

    /**
     * Any generated model carrying OpenAPI {@code date-time} properties serves here; {@code Discount}
     * is one of the smallest. The point is the codec configuration, not the model.
     */
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

    /**
     * Every OpenAPI {@code date-time} property becomes an {@link OffsetDateTime} in the generated
     * Layer-1 models, and a bare {@code ObjectMapper} rejects those outright. {@code ShopResponse} —
     * the only payload the shop slice decodes — happens to carry no timestamp, so nothing exercised
     * this until a domain bucket decoded a real one. These two tests pin the configuration.
     */
    @Test
    void readDecodesGeneratedModelsThatCarryDateTimeProperties() {
        Discount discount = codec.read(MODEL_WITH_TIMESTAMPS_JSON, Discount.class);

        assertEquals(OffsetDateTime.parse("2026-07-20T08:15:00Z"), discount.getStartAt());
        assertEquals("SKU-1", discount.getExternalId());
    }

    @Test
    void readPreservesTheUtcOffsetTheApiSentInsteadOfRewritingItToUtc() {
        Discount discount = codec.read(MODEL_WITH_TIMESTAMPS_JSON, Discount.class);

        assertEquals(OffsetDateTime.parse("2026-07-27T08:15:00+02:00"), discount.getRestoreAt());
        assertEquals(ZoneOffset.ofHours(2), discount.getRestoreAt().getOffset());
    }

    @Test
    void writeEmitsDatesAsIso8601StringsRatherThanEpochNumbers() {
        Discount discount = new Discount();
        discount.setStartAt(OffsetDateTime.parse("2026-07-20T08:15:00Z"));

        String json = codec.write(discount);

        assertTrue(json.contains("\"startAt\":\"2026-07-20T08:15:00Z\""), json);
    }
}
