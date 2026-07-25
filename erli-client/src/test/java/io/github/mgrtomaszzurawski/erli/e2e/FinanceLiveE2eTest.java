package io.github.mgrtomaszzurawski.erli.e2e;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimate;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live proof for the Finance bucket against the real Erli sandbox — the Definition-of-Done e2e
 * requirement (green WireMock alone is not merge-ready, see {@code TESTING.md}).
 *
 * <p>Excluded from the default {@code test} task by its tag; run with {@code ./gradlew :erli-client:e2eTest}
 * and both {@code ERLI_API_KEY} and {@code ERLI_BASE_URL} exported. Skipped automatically when they
 * are absent. The key is read from the environment and never printed.
 */
@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "ERLI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ERLI_BASE_URL", matches = ".+")
class FinanceLiveE2eTest {

    /** "Elementy dekarskie" (roofing components) — a leaf category; non-leaf ids are rejected. */
    private static final String LEAF_CATEGORY_ID = "4";
    private static final String UNIT_PRICE_PLN = "100.00";

    @Test
    void estimatesACommissionLive() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            CommissionEstimate estimate = client.commissions().estimate(
                    CommissionEstimateRequest.builder()
                            .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                            .unitPrice(Money.ofPln(UNIT_PRICE_PLN))
                            .build());

            assertNotNull(estimate.commission());
            assertEquals("PLN", estimate.commission().currency().getCurrencyCode());
            // The spec allows a zero commission, so only non-negativity is a contract; the upper
            // bound catches a grosze/złoty unit mix-up, which would inflate the value 100-fold.
            assertTrue(estimate.commission().amount().compareTo(BigDecimal.ZERO) >= 0,
                    "commission must never be negative, got " + estimate.commission().amount());
            assertTrue(estimate.commission().amount().compareTo(new BigDecimal(UNIT_PRICE_PLN)) < 0,
                    "commission must be a fraction of the unit price, got " + estimate.commission().amount());
        }
    }
}
