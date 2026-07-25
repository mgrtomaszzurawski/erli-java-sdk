package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonCodecTest {

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
}
