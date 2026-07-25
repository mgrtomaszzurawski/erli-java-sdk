package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAccess;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof of the products bucket against the live Erli sandbox.
 *
 * <p>Self-gating: it skips unless {@code ERLI_BASE_URL} and {@code ERLI_API_KEY} are in the environment,
 * so an ordinary {@code ./gradlew test} is unaffected. Run it with the credentials sourced:
 *
 * <pre>{@code
 * set -a; . /workspace/shared/secrets/erli-sandbox.env; set +a
 * ./gradlew :erli-client:test --tests '*ProductLiveSandboxTest*'
 * }</pre>
 *
 * <p>What this proves today is the read path that needs no payload decoding: transport, Bearer auth, the
 * templated and percent-encoded path, and the mapping of a real 404 to "absent" rather than an
 * exception. The write→read seed that would let the bucket observe real product and cursor shapes is
 * deliberately <strong>not</strong> run yet — until the core {@code JsonCodec} fix lands, a create would
 * put a malformed payload on the wire (see the BACKLOG note on {@code fix/core-jsoncodec-java-time}), and
 * seeding the shared sandbox with junk is worse than waiting.
 */
@Tag("e2e")
class ProductLiveSandboxTest {

    private static final String BASE_URL_ENV_VAR = "ERLI_BASE_URL";
    private static final String API_KEY_ENV_VAR = "ERLI_API_KEY";

    /** An id no seller would mint, so the sandbox reliably has no such product. */
    private static final ProductExternalId ABSENT_PRODUCT_ID =
            ProductExternalId.of("erli-java-sdk-absent-probe");

    private static boolean credentialsPresent() {
        return isSet(System.getenv(BASE_URL_ENV_VAR)) && isSet(System.getenv(API_KEY_ENV_VAR));
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    @Test
    void readsThroughTheProductsAccessorAgainstTheLiveSandbox() {
        assumeTrue(credentialsPresent(),
                "Live sandbox credentials absent; set " + BASE_URL_ENV_VAR + " and " + API_KEY_ENV_VAR);

        try (ErliClient client = ErliClient.builder()
                .apiKey(ApiKey.fromEnvironment())
                .baseUrlFromEnvironment()
                .build()) {

            ProductAccess products = client.products();

            // A real 404 from the marketplace must read as "no such product", not as a failure — and
            // reaching a 404 at all proves the request was authenticated and routed correctly.
            assertTrue(products.get(ABSENT_PRODUCT_ID).isEmpty(),
                    "The sandbox unexpectedly holds a product under the probe id");
            assertTrue(products.getDiscount(ABSENT_PRODUCT_ID).isEmpty(),
                    "The sandbox unexpectedly holds a discount under the probe id");
        }
    }
}
