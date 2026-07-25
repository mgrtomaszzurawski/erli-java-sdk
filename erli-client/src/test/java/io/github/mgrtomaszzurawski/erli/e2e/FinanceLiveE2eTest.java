package io.github.mgrtomaszzurawski.erli.e2e;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntryFilter;
import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignCostSummary;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimate;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.PayoutSearch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live proof for the Finance bucket against the real Erli sandbox — the Definition-of-Done e2e
 * requirement (green WireMock alone is not merge-ready, see {@code TESTING.md}). Every one of the
 * bucket's four accessors is exercised.
 *
 * <p>Excluded from the default {@code test} task by its tag; run with
 * {@code ./gradlew :erli-client:e2eTest} and both {@code ERLI_API_KEY} and {@code ERLI_BASE_URL}
 * exported. Skipped automatically when they are absent. The key is read from the environment and
 * never printed.
 *
 * <p>The sandbox shop is empty, so the list operations assert reachability and decoding rather than
 * content; real payloads only exist after Phase 3 seeds data.
 */
@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "ERLI_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ERLI_BASE_URL", matches = ".+")
class FinanceLiveE2eTest {

    /** "Elementy dekarskie" (roofing components) — a leaf category; non-leaf ids are rejected. */
    private static final String LEAF_CATEGORY_ID = "4";
    private static final String UNIT_PRICE_PLN = "100.00";

    /** The API refuses an endDate past its own clock, so stay a day behind to avoid a timezone race. */
    private static final LocalDate END_DATE = LocalDate.now(ZoneId.of("Europe/Warsaw")).minusDays(2);
    private static final LocalDate START_DATE = END_DATE.minusMonths(1);

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

    @Test
    void readsTheCampaignCostSummaryLive() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            CampaignCostSummary summary = client.campaigns().costSummary(START_DATE, END_DATE);

            assertTrue(summary.shopId() > 0, "the summary must name the shop it belongs to");
            assertNotNull(summary.dailyCosts());
            assertNotNull(summary.totalNetCost());
        }
    }

    @Test
    void walksTheCompanyLedgerLive() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            // Terminal operation on a lazy stream: proves the request, the decode and the mapper.
            long entries = client.billing().entries(BillingEntryFilter.all()).limit(5).count();
            long rebates = client.billing().rebates(BillingEntryFilter.all()).limit(5).count();

            assertTrue(entries >= 0);
            assertTrue(rebates >= 0);
        }
    }

    @Test
    void searchesPaymentsAndPayoutsLive() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            long payments = client.payments().searchPayments(PaymentSearch.all()).limit(5).count();
            long payouts = client.payments().searchPayouts(PayoutSearch.all()).limit(5).count();

            assertTrue(payments >= 0);
            assertTrue(payouts >= 0);
        }
    }

    @Test
    void reportsAMissingPaymentAsEmptyRatherThanFailingLive() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            // Id 1 does not exist on the sandbox; the SDK must translate the 404 into an empty result.
            assertTrue(client.payments().findPayment(1L).isEmpty());
            assertTrue(client.payments().findPayout(1L).isEmpty());
        }
    }
}
