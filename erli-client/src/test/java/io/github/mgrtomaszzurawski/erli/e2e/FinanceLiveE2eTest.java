package io.github.mgrtomaszzurawski.erli.e2e;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntry;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntryFilter;
import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignCostSummary;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimate;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import io.github.mgrtomaszzurawski.erli.domain.payments.Payment;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.Payout;
import io.github.mgrtomaszzurawski.erli.domain.payments.PayoutSearch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final LocalDate END_DATE = LocalDate.now(WARSAW).minusDays(2);
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
            // Every row must carry the required cost; vacuous while the shop has no spend, real once
            // it does. The shopId check above is the assertion that bites today.
            summary.dailyCosts().forEach(row -> assertNotNull(row.netShopCost()));
            assertEquals("PLN", summary.totalNetCost().currency().getCurrencyCode());
        }
    }

    @Test
    void rejectsAFutureEndDateLive() {
        // Non-vacuous even on an empty shop: proves the request really reaches the API and that a
        // live 400 maps to the right remediation exception. The spec marks endDate optional; the
        // server both requires it and refuses one past its own clock.
        try (ErliClient client = ErliClient.fromEnvironment()) {
            ErliValidationException failure = assertThrows(ErliValidationException.class,
                    () -> client.campaigns().costSummary(START_DATE, LocalDate.now(WARSAW).plusYears(1)));

            assertEquals(400, failure.details().httpStatus());
            assertNotNull(failure.details().polishMessage(), "Erli returns an operator-facing message");
        }
    }

    @Test
    void walksTheCompanyLedgerLive() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            // The assertion IS that the server accepts our request shape. Both endpoints are strict:
            // a missing simpleFilter, an explicit null, or a sort other than id/DESC all draw a 400,
            // so reaching a decoded result proves the body the SDK builds is the one Erli wants.
            List<BillingEntry> entries = assertDoesNotThrow(
                    () -> client.billing().entries(BillingEntryFilter.all()).limit(5).toList(),
                    "the ledger request shape must be accepted by the live API");
            List<BillingEntry> rebates = assertDoesNotThrow(
                    () -> client.billing().rebates(BillingEntryFilter.all()).limit(5).toList(),
                    "the rebates request shape must be accepted by the live API");

            // Content invariants only bite once the ledger has rows; until then say so honestly by
            // skipping rather than passing on an empty list.
            assumeFalse(entries.isEmpty() && rebates.isEmpty(),
                    "sandbox ledger is empty — mapping stays unproven until Phase 3 seeds data");
            entries.forEach(entry -> assertEquals("PLN", entry.amount().currency().getCurrencyCode()));
            rebates.forEach(entry -> assertNotNull(entry.balanceAfter()));
        }
    }

    @Test
    void searchesPaymentsAndPayoutsLive() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            // Same reasoning as the ledger: reaching a decoded result is the proof. This endpoint is
            // the strictest in the bucket — the type discriminator must be in the BODY, and the spec's
            // query-parameter form draws 400 "type is required" (KNOWN-SERVER-BEHAVIORS.md). So a
            // successful call is exactly what distinguishes the right wire shape from the spec's.
            List<Payment> payments = assertDoesNotThrow(
                    () -> client.payments().searchPayments(PaymentSearch.all()).limit(5).toList(),
                    "the payment search must send 'type' where the live API reads it");
            List<Payout> payouts = assertDoesNotThrow(
                    () -> client.payments().searchPayouts(PayoutSearch.all()).limit(5).toList(),
                    "the payout search must send 'type' where the live API reads it");

            assumeFalse(payments.isEmpty() && payouts.isEmpty(),
                    "sandbox shop has no payments — mapping stays unproven until Phase 3 seeds data");
            payments.forEach(payment -> assertNotNull(payment.status()));
            payouts.forEach(payout -> assertEquals("PLN", payout.amount().currency().getCurrencyCode()));
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
