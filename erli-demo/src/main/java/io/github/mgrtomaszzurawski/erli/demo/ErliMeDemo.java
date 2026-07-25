package io.github.mgrtomaszzurawski.erli.demo;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.domain.shop.Shop;

/**
 * Live end-to-end proof of the Core M1 slice: build a client from the environment
 * ({@code ERLI_BASE_URL} + {@code ERLI_API_KEY}) and call {@code GET /me} against the real sandbox.
 *
 * <p>Run: {@code ./gradlew :erli-demo:run} with the two environment variables set (sourced from
 * {@code /workspace/shared/secrets/erli-sandbox.env}). The API key is never printed — only the
 * non-secret shop fields are.
 */
public final class ErliMeDemo {

    private ErliMeDemo() {
    }

    public static void main(String[] args) {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            Shop shop = client.shop().me();
            System.out.printf(
                    "GET /me OK -> shop id=%d name=%s active=%b matchingPolicy=%s%n",
                    shop.id(), shop.name(), shop.active(), shop.externalMatchingPolicy());
            shop.company().ifPresent(company ->
                    System.out.printf("  company: name=%s nip=%s%n", company.name(), company.nip()));
        }
    }
}
