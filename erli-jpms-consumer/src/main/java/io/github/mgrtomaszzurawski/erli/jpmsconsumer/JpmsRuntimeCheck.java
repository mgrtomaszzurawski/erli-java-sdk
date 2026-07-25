package io.github.mgrtomaszzurawski.erli.jpmsconsumer;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntryFilter;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import io.github.mgrtomaszzurawski.erli.domain.payments.PaymentSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.PayoutSearch;
import io.github.mgrtomaszzurawski.erli.domain.payments.ReturnSearch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the SDK's request-encoding paths from a real JPMS module, on the module path.
 *
 * <p><strong>Why this exists.</strong> Compiling {@link JpmsConsumer} proves the exported surface is
 * self-contained, but it cannot prove the SDK <em>works</em> as a module. Jackson serializes request
 * bodies reflectively, and reflection into a package that is neither exported nor {@code opens} fails
 * with {@code InaccessibleObjectException} — at runtime, on the module path only. Unit tests never see
 * it, because Gradle patches them into the module and runs them on the classpath.
 *
 * <p>That gap shipped a real defect: every {@code /payments/operations/_search} call failed with
 * "Failed to encode request body" while the whole suite stayed green. This check closes it for every
 * bucket: any hand-written request body in a non-opened package fails the build here.
 *
 * <p>Each call is pointed at an unroutable address, so the request never leaves the machine. Reaching
 * the transport at all means encoding succeeded, which is what is under test; a connection failure is
 * the expected outcome.
 */
public final class JpmsRuntimeCheck {

    /** Port 1 is reserved and never listening, so the connection fails fast without any traffic. */
    private static final String UNROUTABLE_BASE_URL = "http://127.0.0.1:1";
    private static final String PLACEHOLDER_KEY = "jpms:check";
    private static final String ENCODE_FAILURE_MARKER = "Failed to encode request body";
    private static final int FAILURE_EXIT_CODE = 1;

    private JpmsRuntimeCheck() {
    }

    public static void main(String[] args) {
        List<String> failures = run();
        if (failures.isEmpty()) {
            System.out.println("JPMS runtime check OK — every request body encodes on the module path.");
            return;
        }
        failures.forEach(failure -> System.err.println("JPMS runtime check FAILED: " + failure));
        System.exit(FAILURE_EXIT_CODE);
    }

    /** Exercise one write path per bucket that builds a request body; returns the encoding failures. */
    private static List<String> run() {
        List<String> failures = new ArrayList<>();
        try (ErliClient client = ErliClient.builder()
                .baseUrl(UNROUTABLE_BASE_URL)
                .apiKey(ApiKey.of(PLACEHOLDER_KEY))
                .build()) {

            check(failures, "commissions.estimate", () -> client.commissions().estimate(
                    CommissionEstimateRequest.builder()
                            .categoryId(CategoryId.of("4"))
                            .unitPrice(Money.ofPln("100.00"))
                            .build()));
            check(failures, "billing.entries", () ->
                    client.billing().entries(BillingEntryFilter.all()).findFirst());
            check(failures, "billing.rebates", () ->
                    client.billing().rebates(BillingEntryFilter.all()).findFirst());
            check(failures, "payments.searchPayments", () ->
                    client.payments().searchPayments(PaymentSearch.all()).findFirst());
            check(failures, "payments.searchPayouts", () ->
                    client.payments().searchPayouts(PayoutSearch.all()).findFirst());
            check(failures, "payments.searchReturns", () -> client.payments().searchReturns(
                    ReturnSearch.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).findFirst());
        }
        return failures;
    }

    /**
     * Run one call and record a failure only when it could not encode. Any other outcome — including
     * the expected connection error — means reflection into the request body worked.
     */
    private static void check(List<String> failures, String operation, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException failure) {
            if (containsEncodeFailure(failure)) {
                failures.add(operation + " -> " + rootCauseOf(failure));
            }
        }
    }

    private static boolean containsEncodeFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.contains(ENCODE_FAILURE_MARKER)) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    private static String rootCauseOf(Throwable failure) {
        Throwable root = failure;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName() + ": " + root.getMessage();
    }
}
