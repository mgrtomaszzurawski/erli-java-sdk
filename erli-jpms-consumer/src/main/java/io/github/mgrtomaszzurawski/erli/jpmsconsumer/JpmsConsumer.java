package io.github.mgrtomaszzurawski.erli.jpmsconsumer;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliException;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.domain.shop.Shop;

/**
 * Exercises the public API from a real JPMS module. If this compiles, the exported surface is
 * reachable and self-contained; the internal transport and generated models stay hidden.
 */
public final class JpmsConsumer {

    private JpmsConsumer() {
    }

    public static String touchPublicSurface(String baseUrl, String rawApiKey) {
        // Core value type is reachable.
        OrderId orderId = OrderId.of("ORD-1");
        try (ErliClient client = ErliClient.builder()
                .baseUrl(baseUrl)
                .apiKey(ApiKey.of(rawApiKey))
                .build()) {
            Shop shop = client.shop().me();
            return shop.name() + " / " + orderId.value();
        } catch (ErliException failure) {
            return failure.getMessage();
        }
    }
}
