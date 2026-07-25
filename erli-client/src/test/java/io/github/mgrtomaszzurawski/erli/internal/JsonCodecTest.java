package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.rest.model.Order;
import io.github.mgrtomaszzurawski.erli.rest.model.OrderUpdate;
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

    private static final String ORDER_WITH_DATES = """
            {"id":"221201x12345","created":"2026-07-20T08:15:30Z","updated":"2026-07-21T09:16:31Z"}""";

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
    void decodesOffsetDateTime() {
        // GET /me carries no date-time property, so nothing exercised the JSR-310 wiring until the
        // first dated payload — an order's created/updated.
        Order decoded = codec.read(ORDER_WITH_DATES, Order.class);

        assertEquals(OffsetDateTime.of(2026, 7, 20, 8, 15, 30, 0, ZoneOffset.UTC), decoded.getCreated());
        assertEquals(OffsetDateTime.of(2026, 7, 21, 9, 16, 31, 0, ZoneOffset.UTC), decoded.getUpdated());
    }

    @Test
    void writesDateTimeAsIsoStringNotANumericTimestamp() {
        Order order = new Order().created(OffsetDateTime.of(2026, 7, 20, 8, 15, 30, 0, ZoneOffset.UTC));

        String json = codec.write(order);

        assertTrue(json.contains("\"created\":\"2026-07-20T08:15:30Z\""),
                "expected an ISO-8601 date-time, got: " + json);
    }

    @Test
    void preservesANonUtcOffsetInsteadOfRewritingItToUtc() {
        // Same instant either way; the offset is the part that would be silently lost.
        Order decoded = codec.read(
                "{\"id\":\"221201x1\",\"created\":\"2026-07-20T10:15:30+02:00\"}", Order.class);

        assertEquals(OffsetDateTime.of(2026, 7, 20, 10, 15, 30, 0, ZoneOffset.ofHours(2)),
                decoded.getCreated());
        assertEquals(ZoneOffset.ofHours(2), decoded.getCreated().getOffset());
    }

    @Test
    void writesRequestBodyWithoutNullValuedOptionalProperties() {
        // On a PATCH an explicit null is a request to clear the field, so unset properties must be
        // omitted rather than serialized as null.
        OrderUpdate update = new OrderUpdate().externalOrderId("erp-42");

        assertEquals("{\"externalOrderId\":\"erp-42\"}", codec.write(update));
    }
}
