package io.github.mgrtomaszzurawski.erli.domain.delivery;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live end-to-end proof of the delivery area against the Erli sandbox. Tagged {@code e2e}, so it is
 * excluded from the normal {@code test} run and executed by {@code ./gradlew :erli-client:e2eTest}
 * with {@code ERLI_BASE_URL} + {@code ERLI_API_KEY} set.
 *
 * <p>Read-only on purpose. Price lists are shop configuration, not sample data, so creating one would
 * leave the shared sandbox shop dirty for every other bucket; the write path is covered by
 * verify-on-write against WireMock and is scheduled for the Phase 3 live sweep.
 *
 * <p>Observed 2026-07-25: the sandbox shop has <em>no</em> price lists at all — not even the default
 * {@code "*"} one. See {@code KNOWN-SERVER-BEHAVIORS.md}.
 */
@Tag("e2e")
class PriceListE2ETest {

    @Test
    void readsPriceListsAndTheirDetailsFromTheLiveSandbox() {
        assumeTrue(isSet("ERLI_BASE_URL") && isSet("ERLI_API_KEY"), "sandbox environment not configured");

        try (ErliClient client = ErliClient.fromEnvironment()) {
            List<PriceListSummary> summaries = client.delivery().priceLists();
            List<PriceList> details = client.delivery().priceListDetails(PriceListQuery.none());

            // Both calls reached the live API: a wrong base URL raises ErliTransportException and a
            // rejected credential ErliAuthException, so a decoded list at all is the proof. The shop
            // itself has no price lists configured (observed 2026-07-25), so emptiness is expected —
            // asserting non-emptiness here would fail for a correct SDK.
            assertNotNull(summaries, "price list summaries");
            assertEquals(summaries.size(), details.size(), "both endpoints describe the same lists");

            // Whatever the shop does have must map completely; nothing is asserted into existence.
            for (PriceList priceList : details) {
                assertNotNull(priceList.name(), "name");
                assertNotNull(priceList.createdAt(), "createdAt");
                assertFalse(priceList.prices().isEmpty(), "a price list always carries a delivery price");
                assertNotNull(priceList.prices().get(0).basePrice(), "basePrice");
            }
        }
    }

    private static boolean isSet(String variable) {
        String value = System.getenv(variable);
        return value != null && !value.isBlank();
    }
}
