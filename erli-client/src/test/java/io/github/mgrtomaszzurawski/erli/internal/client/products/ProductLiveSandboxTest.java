package io.github.mgrtomaszzurawski.erli.internal.client.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliApiException;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.products.DispatchTime;
import io.github.mgrtomaszzurawski.erli.domain.products.Product;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAccess;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductContent;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDraft;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFilter;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFilterField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductImage;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductPatch;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSearchRequest;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductStatus;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductUpdateResult;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * End-to-end proof of the products bucket against the live Erli sandbox, including the write→read seed.
 *
 * <p>Self-gating: skips unless {@code ERLI_BASE_URL} and {@code ERLI_API_KEY} are in the environment, so
 * an ordinary {@code ./gradlew test} is unaffected. Run it with the credentials sourced:
 *
 * <pre>{@code
 * set -a; . /workspace/shared/secrets/erli-sandbox.env; set +a
 * ./gradlew :erli-client:test --tests '*ProductLiveSandboxTest*'
 * }</pre>
 *
 * <p><strong>This test writes to the shared sandbox.</strong> Bucket A is the fan-out's seeding enabler
 * (FANOUT-PLAN), so the product it creates is deliberately left in place under a self-describing id for
 * buckets B/C/E to observe real order/payment/shipping shapes against. It is idempotent: a re-run
 * updates the same product rather than creating a second one.
 *
 * <p>Creation is asynchronous — Erli answers {@code 202 Accepted} and materialises the product a moment
 * later — so the read back polls briefly rather than assuming immediate visibility.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductLiveSandboxTest {

    private static final String BASE_URL_ENV_VAR = "ERLI_BASE_URL";
    private static final String API_KEY_ENV_VAR = "ERLI_API_KEY";

    /** Self-describing so anyone finding it in the sandbox knows what created it and why. */
    private static final ProductExternalId SEED_PRODUCT_ID =
            ProductExternalId.of("erli-java-sdk-e2e-seed-1");

    /** An id no seller would mint, so the sandbox reliably has no such product. */
    private static final ProductExternalId ABSENT_PRODUCT_ID =
            ProductExternalId.of("erli-java-sdk-absent-probe");

    private static final String SEED_NAME = "erli-java-sdk e2e seed product";
    private static final String SEED_IMAGE = "https://picsum.photos/seed/erli-sdk/800/800.jpg";
    private static final Money SEED_PRICE = Money.ofPln("100.00");
    private static final int SEED_STOCK = 10;
    private static final int RESTOCKED = 7;

    private static final int VISIBILITY_ATTEMPTS = 8;
    private static final Duration VISIBILITY_PAUSE = Duration.ofSeconds(3);

    /**
     * The sandbox rate-limits, and a 429 is not retried by the transport on a write
     * ({@code retryPost=false}), so the live test backs off itself. Nothing about the SDK is under test
     * here — this only stops a shared, throttled environment from failing an otherwise good run.
     */
    private static final int RATE_LIMIT_ATTEMPTS = 5;
    private static final Duration RATE_LIMIT_PAUSE = Duration.ofSeconds(20);
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private static boolean credentialsPresent() {
        return isSet(System.getenv(BASE_URL_ENV_VAR)) && isSet(System.getenv(API_KEY_ENV_VAR));
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

    private static ErliClient openClient() {
        assumeTrue(credentialsPresent(),
                "Live sandbox credentials absent; set " + BASE_URL_ENV_VAR + " and " + API_KEY_ENV_VAR);
        return ErliClient.builder()
                .apiKey(ApiKey.fromEnvironment())
                .baseUrlFromEnvironment()
                .build();
    }

    private static ProductDraft seedDraft() {
        return ProductDraft.of(ProductContent.builder()
                .name(SEED_NAME)
                .price(SEED_PRICE)
                .stock(SEED_STOCK)
                .status(ProductStatus.INACTIVE)
                .dispatchTime(DispatchTime.ofDays(1))
                .images(List.of(ProductImage.of(SEED_IMAGE)))
                .build());
    }

    /**
     * Poll until the asynchronously-created product satisfies {@code ready}, or give up.
     *
     * <p>Erli materialises a product in stages, and this test exists partly to pin that down: the
     * {@code 202} returns first, {@code GET} starts answering a moment later, the images finish
     * ingesting after that (the marketplace downloads and re-hosts them), and {@code _search} indexes it
     * later still. Asserting immediately after the write is the classic way to get a flaky live test.
     */
    private static Optional<Product> awaitProduct(ProductAccess products, ProductExternalId externalId,
            java.util.function.Predicate<Product> ready) {
        for (int attempt = 0; attempt < VISIBILITY_ATTEMPTS; attempt++) {
            Optional<Product> found = withRateLimitBackoff(() -> products.get(externalId)).filter(ready);
            if (found.isPresent()) {
                return found;
            }
            if (!sleepBriefly()) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Poll until the product is findable through {@code _search}, whose index lags {@code GET}. */
    private static List<Product> awaitSearchable(ProductAccess products, ProductExternalId externalId) {
        for (int attempt = 0; attempt < VISIBILITY_ATTEMPTS; attempt++) {
            List<Product> matched = withRateLimitBackoff(() -> products.search(ProductSearchRequest.builder()
                    .filter(ProductFilter.equalTo(ProductFilterField.EXTERNAL_ID, externalId.value()))
                    .build()).toList());
            if (!matched.isEmpty()) {
                return matched;
            }
            if (!sleepBriefly()) {
                return List.of();
            }
        }
        return List.of();
    }

    /** Run {@code call}, backing off and retrying while the sandbox is throttling. */
    private static <T> T withRateLimitBackoff(java.util.function.Supplier<T> call) {
        ErliApiException lastThrottle = null;
        for (int attempt = 0; attempt < RATE_LIMIT_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (ErliApiException failure) {
                if (failure.details().httpStatus() != HTTP_TOO_MANY_REQUESTS) {
                    throw failure;
                }
                lastThrottle = failure;
                sleepFor(RATE_LIMIT_PAUSE);
            }
        }
        throw new AssertionError("The sandbox kept rate-limiting after "
                + RATE_LIMIT_ATTEMPTS + " attempts", lastThrottle);
    }

    private static boolean sleepBriefly() {
        return sleepFor(VISIBILITY_PAUSE);
    }

    // A live-sandbox visibility pause: waits for the real server to make a just-written product
    // readable before the next call. Not deterministic state, so Awaitility buys nothing here.
    @SuppressWarnings("java:S2925")
    private static boolean sleepFor(Duration pause) {
        try {
            Thread.sleep(pause.toMillis());
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Test
    @Order(1)
    void createsThenReadsBackTheSeedProduct() {
        try (ErliClient client = openClient()) {
            ProductAccess products = client.products();

            // Idempotence is decided by asking whether the seed exists, not by interpreting a failure.
            // Erli answers 409 for "already exists" but also 429 for a rate limit, and the SDK maps both
            // into the same remediation group — so catching the exception would let a throttled run pass
            // as a successful re-run.
            if (withRateLimitBackoff(() -> products.get(SEED_PRODUCT_ID)).isEmpty()) {
                withRateLimitBackoff(() -> {
                    products.create(SEED_PRODUCT_ID, seedDraft());
                    return null;
                });
            } else {
                withRateLimitBackoff(() -> products.update(SEED_PRODUCT_ID, ProductPatch.builder()
                        .content(ProductContent.builder().stock(SEED_STOCK).build())
                        .build()));
            }

            // Wait for the image too: the marketplace re-hosts it, so it lands after the product does.
            Product seeded = awaitProduct(products, SEED_PRODUCT_ID, product -> !product.images().isEmpty())
                    .orElseThrow(() -> new AssertionError(
                            "The seed product was accepted but never became readable with its image"));

            assertEquals(SEED_PRODUCT_ID.value(), seeded.externalId().value());
            assertEquals(SEED_NAME, seeded.name());
            // The value the SDK sent as minor units must come back as the same Money.
            assertEquals(SEED_PRICE.amount(), seeded.price().amount());
            assertEquals("PLN", seeded.price().currency().getCurrencyCode());
            assertTrue(seeded.marketplaceId() > 0, "the marketplace assigns its own id");
            assertTrue(seeded.created().getYear() >= 2026, "created must decode as a real timestamp");
            assertEquals(1, seeded.dispatchTime().period());
            assertEquals(SEED_IMAGE, seeded.images().get(0).url(), "the cover image must round trip");
            assertTrue(seeded.images().get(0).internalUrl().isPresent(),
                    "the marketplace re-hosts the image and reports its own URL");
        }
    }

    @Test
    @Order(2)
    void updatesOnlyTheFieldThePatchNamed() {
        try (ErliClient client = openClient()) {
            ProductAccess products = client.products();

            ProductUpdateResult result = withRateLimitBackoff(() -> products.update(SEED_PRODUCT_ID,
                    ProductPatch.builder()
                            .content(ProductContent.builder().stock(RESTOCKED).build())
                            .build()));

            assertTrue(result.changed(ProductField.STOCK),
                    "the marketplace should report stock as changed, got " + result.updatedFields());

            Product after = withRateLimitBackoff(() -> products.get(SEED_PRODUCT_ID)).orElseThrow();
            assertEquals(RESTOCKED, after.stock());
            // The whole point of the three-state patch: everything untouched must survive.
            assertEquals(SEED_NAME, after.name());
            assertEquals(SEED_PRICE.amount(), after.price().amount());
            assertTrue(after.images().size() >= 1, "an unmentioned image list must not be cleared");
        }
    }

    @Test
    @Order(3)
    void findsTheSeedThroughSearchAndWalksTheClientSideCursor() {
        try (ErliClient client = openClient()) {
            ProductAccess products = client.products();

            List<Product> matched = awaitSearchable(products, SEED_PRODUCT_ID);

            assertEquals(1, matched.size(), "the filter should match exactly the seed product");
            assertEquals(SEED_PRODUCT_ID.value(), matched.get(0).externalId().value());

            // Laziness: a one-page-sized walk must not spin. A tiny page size with limit(1) proves the
            // stream stops pulling as soon as the consumer does.
            long firstOnly = withRateLimitBackoff(() -> products
                    .search(ProductSearchRequest.builder().pageSize(1).build()).limit(1).count());
            assertEquals(1, firstOnly);
        }
    }

    @Test
    @Order(4)
    void absentProductAndAbsentDiscountReadAsEmptyRatherThanFailing() {
        try (ErliClient client = openClient()) {
            ProductAccess products = client.products();

            // A real 404 must read as "no such product" — and reaching a 404 proves the request was
            // authenticated and routed correctly.
            assertTrue(withRateLimitBackoff(() -> products.get(ABSENT_PRODUCT_ID)).isEmpty(),
                    "The sandbox unexpectedly holds a product under the probe id");
            assertTrue(withRateLimitBackoff(() -> products.getDiscount(ABSENT_PRODUCT_ID)).isEmpty(),
                    "The sandbox unexpectedly holds a discount under the probe id");
        }
    }
}
