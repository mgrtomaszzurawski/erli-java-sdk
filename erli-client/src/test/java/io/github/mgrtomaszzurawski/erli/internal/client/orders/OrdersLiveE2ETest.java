package io.github.mgrtomaszzurawski.erli.internal.client.orders;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.orders.Order;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderSearchRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live proof that the Orders bucket works against the real Erli sandbox, not just against WireMock.
 * Green stubs prove shape; only the live environment proves truth (see {@code TESTING.md}).
 *
 * <p>Self-gating: the test is skipped unless {@code ERLI_API_KEY} and {@code ERLI_BASE_URL} are in the
 * environment, so an ordinary {@code ./gradlew build} without credentials stays green. Source them
 * from {@code /workspace/shared/secrets/erli-sandbox.env}; the key is never printed or asserted on.
 *
 * <p>The sandbox shop is empty and inactive, so the search legitimately returns nothing. This test
 * therefore proves the round-trip — request accepted, body shape understood, cursor walk terminates —
 * and asserts the deep field mapping only when the shop actually has an order. The Phase 3 live
 * write-read sweep, once products seed data, is what will exercise a populated payload.
 */
@Tag("e2e")
@EnabledIfEnvironmentVariable(named = ApiKey.API_KEY_ENV_VAR, matches = ".+")
@EnabledIfEnvironmentVariable(named = ErliClient.BASE_URL_ENV_VAR, matches = ".+")
class OrdersLiveE2ETest {

    private static final int SAMPLE_SIZE = 5;
    private static final int RATE_LIMIT_ATTEMPTS = 4;
    private static final Duration RATE_LIMIT_BACKOFF = Duration.ofSeconds(2);

    /**
     * The sandbox rate-limits repeated searches, and {@code _search} is a read Erli exposes as a
     * {@code POST} — so the default policy will not retry it and a 429 aborts the call. Enabling
     * {@code retryPost} here is not a test workaround: it is the configuration {@code docs/orders.md}
     * recommends for exactly this case, so the live run exercises what a real unattended sync would do.
     */
    private static ErliClient liveClient() {
        return ErliClient.builder()
                .apiKey(ApiKey.fromEnvironment())
                .baseUrlFromEnvironment()
                .retryPolicy(RetryPolicy.builder()
                        .maxAttempts(RATE_LIMIT_ATTEMPTS)
                        .baseDelay(RATE_LIMIT_BACKOFF)
                        .retryPost(true)
                        .build())
                .build();
    }

    /**
     * The round-trip itself: the request is accepted, the bare-array body decodes, and the cursor walk
     * terminates rather than looping. This holds on an empty shop, so it is a real assertion today.
     */
    @Test
    void completesASearchRoundTripAgainstTheLiveSandbox() {
        try (ErliClient client = liveClient()) {
            List<Order> sample = client.orders()
                    .search(OrderSearchRequest.builder().pageSize(SAMPLE_SIZE).build())
                    .limit(SAMPLE_SIZE)
                    .toList();

            // Not a tautology: an unterminated cursor walk would hang or overrun instead of returning,
            // and a misread body would have thrown during decoding.
            assertTrue(sample.size() <= SAMPLE_SIZE,
                    "the live walk returned more than the page it was asked for");
        }
    }

    /**
     * The deep field mapping, which needs real data. The sandbox shop is empty today, so this reports
     * SKIPPED rather than passing on an empty loop — a green tick here must mean a live payload was
     * actually inspected.
     */
    @Test
    void mapsEveryRequiredFieldOfALiveOrder() {
        try (ErliClient client = liveClient()) {
            List<Order> sample = client.orders()
                    .search(OrderSearchRequest.builder().pageSize(SAMPLE_SIZE).build())
                    .limit(SAMPLE_SIZE)
                    .toList();

            assumeFalse(sample.isEmpty(), "sandbox shop has no orders yet — nothing to verify");

            for (Order order : sample) {
                assertNotNull(order.id(), "live order is missing its id");
                assertNotNull(order.status(), "live order is missing its status");
                assertNotNull(order.sellerStatus(), "live order is missing its sellerStatus");
                assertNotNull(order.totalPrice().currency(), "live order is missing its currency");
                assertNotNull(order.created(), "live order is missing its created timestamp");
                assertNotNull(order.delivery().typeId(), "live order is missing its delivery method");
            }
        }
    }

    /**
     * Write-side round-trip is not covered live: with an empty shop there is no order safe to update,
     * and this test must never invent one. Reports SKIPPED until Phase 3 seeds data.
     */
    @Test
    void fetchesASingleOrderById() {
        try (ErliClient client = liveClient()) {
            Optional<Order> first = client.orders()
                    .search(OrderSearchRequest.builder().pageSize(1).build())
                    .findFirst();

            assumeTrue(first.isPresent(), "sandbox shop has no orders yet — nothing to fetch by id");

            Order fetched = client.orders().byId(first.get().id());

            assertEquals(first.get().id().value(), fetched.id().value());
            // Prove the single-order endpoint really was read, not just echoed back.
            assertEquals(first.get().created(), fetched.created());
            assertEquals(first.get().totalPrice(), fetched.totalPrice());
        }
    }
}
