package io.github.mgrtomaszzurawski.erli.internal.client.orders;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.domain.orders.Order;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderSearchRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void searchesOrdersOnTheLiveSandbox() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            List<Order> sample = client.orders()
                    .search(OrderSearchRequest.builder().pageSize(SAMPLE_SIZE).build())
                    .limit(SAMPLE_SIZE)
                    .toList();

            assertNotNull(sample, "the live search must return a list, even an empty one");
            assertTrue(sample.size() <= SAMPLE_SIZE, "the stream must respect the limit");

            // Everything the mapper marks required must genuinely be present on a live payload.
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

    @Test
    void fetchesASingleOrderByIdWhenTheSandboxHasOne() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            Optional<Order> first = client.orders()
                    .search(OrderSearchRequest.builder().pageSize(1).build())
                    .findFirst();

            // An empty sandbox is the expected state today; there is nothing to round-trip yet.
            first.ifPresent(order ->
                    assertEquals(order.id().value(), client.orders().byId(order.id()).id().value()));
        }
    }
}
