package io.github.mgrtomaszzurawski.erli.internal.client.commissions;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliApiException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimate;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify-on-write coverage for the commissions slice plus the Finance bucket's mandatory error-path
 * table (see {@code TESTING.md}). Asserts the request the SDK produced, not only the response it
 * decoded.
 */
class CommissionsAccessImplTest {

    private static final String ESTIMATE_PATH = "/commissions/_estimate";
    private static final String API_KEY_VALUE = "100007:test-secret";
    private static final String USER_AGENT_PATTERN = "erli-java-sdk/.*";
    private static final String LEAF_CATEGORY_ID = "4";
    private static final String NON_LEAF_CATEGORY_ID = "1";
    private static final String UNIT_PRICE_PLN = "100.00";

    private static final String EXPECTED_REQUEST_BODY = "{\"categoryId\":4,\"quantity\":1,\"unitPrice\":10000}";
    private static final String SINGLE_UNIT_RESPONSE_BODY = "{\"commission\":1168}";
    private static final String THREE_UNIT_RESPONSE_BODY = "{\"commission\":3504}";

    /**
     * Real body observed live on the sandbox, 2026-07-25: {@code POST /commissions/_estimate} with a
     * non-leaf categoryId. Note it carries {@code name} and a nested object {@code payload} — richer
     * than the published {@code Error} schema.
     */
    private static final String NON_LEAF_ERROR_BODY = """
            {"name":"ValidationFailure","message":"Wystąpił problem z walidacją danych, \
            categoryId: invalid query for non-leaf category","failureType":"validation",\
            "polishMessage":"Problem z walidacją, sprawdź pola",\
            "payload":{"details":{"categoryId":"invalid query for non-leaf category"}},\
            "spanId":"lmtpsZ6QF4CBM","traceId":"lmtpsZ6QF4CBM"}""";

    private static final String OBSERVED_TRACE_ID = "lmtpsZ6QF4CBM";

    private WireMockServer server;
    private ErliClient client;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = clientWith(RetryPolicy.defaultPolicy());
    }

    @AfterEach
    void stopServer() {
        client.close();
        server.stop();
    }

    private ErliClient clientWith(RetryPolicy retryPolicy) {
        return ErliClient.builder()
                .baseUrl(server.baseUrl())
                .apiKey(ApiKey.of(API_KEY_VALUE))
                .retryPolicy(retryPolicy)
                .build();
    }

    /** Fast, deterministic retry so the 5xx row does not sleep for real backoff durations. */
    private static RetryPolicy fastRetry() {
        return RetryPolicy.builder()
                .baseDelay(Duration.ofMillis(1))
                .randomGenerator(new Random(0))
                .build();
    }

    private CommissionEstimateRequest leafRequest() {
        return CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                .unitPrice(Money.ofPln(UNIT_PRICE_PLN))
                .build();
    }

    @Test
    void sendsGroszeAndMapsCommissionBackToMoney() {
        // Values observed live: 100.00 PLN in category 4 -> 1168 grosze (2026-07-25).
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(200).withBody(SINGLE_UNIT_RESPONSE_BODY)));

        CommissionEstimate estimate = client.commissions().estimate(leafRequest());

        assertEquals(Money.ofPln("11.68"), estimate.commission());
        // Strict JSON equality: an extra key, or an explicit "quantity":null, fails here.
        server.verify(postRequestedFor(urlEqualTo(ESTIMATE_PATH))
                .withHeader("Authorization", equalTo("Bearer " + API_KEY_VALUE))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("User-Agent", matching(USER_AGENT_PATTERN))
                .withRequestBody(equalToJson(EXPECTED_REQUEST_BODY)));
    }

    @Test
    void sendsTheRequestedQuantityAndMapsTheScaledCommission() {
        // Observed live: quantity 3 at the same unit price -> 3504 grosze (3 x 1168).
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(200).withBody(THREE_UNIT_RESPONSE_BODY)));

        CommissionEstimate estimate = client.commissions().estimate(CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                .unitPrice(Money.ofPln(UNIT_PRICE_PLN))
                .quantity(3)
                .build());

        assertEquals(Money.ofPln("35.04"), estimate.commission());
        server.verify(postRequestedFor(urlEqualTo(ESTIMATE_PATH))
                .withHeader("Authorization", equalTo("Bearer " + API_KEY_VALUE))
                .withHeader("User-Agent", matching(USER_AGENT_PATTERN))
                .withRequestBody(equalToJson("{\"categoryId\":4,\"quantity\":3,\"unitPrice\":10000}")));
    }

    @Test
    void mapsNonLeafCategoryRejectionToValidationExceptionPreservingTheTrace() {
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(400).withBody(NON_LEAF_ERROR_BODY)));

        ErliValidationException failure = assertThrows(ErliValidationException.class,
                () -> client.commissions().estimate(CommissionEstimateRequest.builder()
                        .categoryId(CategoryId.of(NON_LEAF_CATEGORY_ID))
                        .unitPrice(Money.ofPln(UNIT_PRICE_PLN))
                        .build()));

        assertEquals(OBSERVED_TRACE_ID, failure.details().traceId());
        assertEquals(OBSERVED_TRACE_ID, failure.details().spanId());
        assertEquals("validation", failure.details().failureType());
        assertEquals("Problem z walidacją, sprawdź pola", failure.details().polishMessage());
    }

    /**
     * The mandatory error-path table for the Finance bucket: every status this bucket can surface,
     * mapped to its remediation exception through the real {@code HttpRuntime.post} path (not the
     * mapper in isolation).
     */
    @ParameterizedTest(name = "HTTP {0} -> {1}")
    @CsvSource({
            "401, io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException",
            "403, io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException",
            "404, io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException",
            "400, io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException",
            "409, io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException",
            "422, io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException",
            "500, io.github.mgrtomaszzurawski.erli.core.error.ErliServerException",
            "503, io.github.mgrtomaszzurawski.erli.core.error.ErliServerException",
    })
    void mapsEveryErrorStatusToItsRemediationException(int httpStatus, Class<? extends ErliApiException> expected) {
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(httpStatus)
                        .withBody("{\"message\":\"failure\",\"httpCode\":" + httpStatus + "}")));
        // 5xx is retried before it surfaces, so use the fast policy for every row.
        try (ErliClient retryingClient = clientWith(fastRetry())) {
            ErliApiException failure = assertThrows(ErliApiException.class,
                    () -> retryingClient.commissions().estimate(leafRequest()));

            assertInstanceOf(expected, failure);
            assertEquals(httpStatus, failure.details().httpStatus());
        }
    }

    @Test
    void mapsANonJsonErrorBodyAndKeepsItForDiagnostics() {
        String htmlBody = "<html><body>502 Bad Gateway</body></html>";
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(502).withBody(htmlBody)));

        try (ErliClient retryingClient = clientWith(fastRetry())) {
            ErliServerException failure = assertThrows(ErliServerException.class,
                    () -> retryingClient.commissions().estimate(leafRequest()));

            assertEquals(htmlBody, failure.details().rawBody());
        }
    }

    @Test
    void doesNotRetryTheNonIdempotentPostOnAValidationError() {
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(400).withBody(NON_LEAF_ERROR_BODY)));

        assertThrows(ErliValidationException.class, () -> client.commissions().estimate(leafRequest()));

        server.verify(1, postRequestedFor(urlEqualTo(ESTIMATE_PATH)));
    }

    @Test
    void rejectsANonPlnUnitPriceBeforeTouchingTheWire() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> client.commissions().estimate(CommissionEstimateRequest.builder()
                        .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                        .unitPrice(Money.of(UNIT_PRICE_PLN, "EUR"))
                        .build()));

        assertTrue(failure.getMessage().contains("unitPrice"), failure.getMessage());
        assertTrue(failure.getMessage().contains("PLN"), failure.getMessage());
        server.verify(0, postRequestedFor(urlEqualTo(ESTIMATE_PATH)));
    }

    @Test
    void rejectsANonNumericCategoryBeforeTouchingTheWire() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> client.commissions().estimate(CommissionEstimateRequest.builder()
                        .categoryId(CategoryId.of("not-a-number"))
                        .unitPrice(Money.ofPln(UNIT_PRICE_PLN))
                        .build()));

        assertTrue(failure.getMessage().contains("categoryId"), failure.getMessage());
        server.verify(0, postRequestedFor(urlEqualTo(ESTIMATE_PATH)));
    }
}
