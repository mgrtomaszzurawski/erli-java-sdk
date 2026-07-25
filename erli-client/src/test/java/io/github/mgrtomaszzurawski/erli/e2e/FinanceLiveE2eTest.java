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
 * <p>Skipped automatically unless {@code ERLI_API_KEY} and {@code ERLI_BASE_URL} are exported; source
 * them from {@code /workspace/shared/secrets/erli-sandbox.env}. The key is never printed.
 */
@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "ERLI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ERLI_BASE_URL", matches = ".+")
class FinanceLiveE2eTest {

    /** "Elementy dekarskie" — a leaf category on the sandbox; non-leaf ids are rejected by the API. */
    private static final String LEAF_CATEGORY_ID = "4";

    @Test
    void estimatesACommissionLive() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            CommissionEstimate estimate = client.commissions().estimate(
                    CommissionEstimateRequest.builder()
                            .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                            .unitPrice(Money.ofPln("100.00"))
                            .build());

            assertNotNull(estimate.commission());
            assertEquals("PLN", estimate.commission().currency().getCurrencyCode());
            assertTrue(estimate.commission().amount().compareTo(BigDecimal.ZERO) > 0,
                    "a 100 PLN listing should carry a positive commission");
        }
    }
}
