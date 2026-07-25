package io.github.mgrtomaszzurawski.erli.internal.client.commissions;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimate;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verify-on-write coverage for the commissions slice (see {@code TESTING.md}). Asserts the request
 * the SDK produced, not only the response it decoded.
 */
class CommissionsAccessImplTest {

    private static final String ESTIMATE_PATH = "/commissions/_estimate";
    private static final String API_KEY_VALUE = "100007:test-secret";
    private static final String LEAF_CATEGORY_ID = "4";

    private WireMockServer server;
    private ErliClient client;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = ErliClient.builder()
                .baseUrl(server.baseUrl())
                .apiKey(ApiKey.of(API_KEY_VALUE))
                .build();
    }

    @AfterEach
    void stopServer() {
        client.close();
        server.stop();
    }

    @Test
    void sendsGroszeAndMapsCommissionBackToMoney() {
        // Body observed live on the sandbox: {"categoryId":4,"quantity":1,"unitPrice":10000} -> 1168.
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(200).withBody("{\"commission\":1168}")));

        CommissionEstimate estimate = client.commissions().estimate(
                CommissionEstimateRequest.builder()
                        .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                        .unitPrice(Money.ofPln("100.00"))
                        .build());

        assertEquals(Money.ofPln("11.68"), estimate.commission());
        server.verify(postRequestedFor(urlEqualTo(ESTIMATE_PATH))
                .withHeader("Authorization", equalTo("Bearer " + API_KEY_VALUE))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("{\"categoryId\":4,\"quantity\":1,\"unitPrice\":10000}")));
    }

    @Test
    void sendsTheRequestedQuantity() {
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(200).withBody("{\"commission\":3504}")));

        client.commissions().estimate(CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                .unitPrice(Money.ofPln("100.00"))
                .quantity(3)
                .build());

        server.verify(postRequestedFor(urlEqualTo(ESTIMATE_PATH))
                .withRequestBody(matchingJsonPath("$.quantity", equalTo("3"))));
    }

    @Test
    void neverSendsAnExplicitNull() {
        // The API rejects an explicitly-null optional instead of treating it as absent, so the codec
        // must omit unset fields entirely (KNOWN-SERVER-BEHAVIORS.md).
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(200).withBody("{\"commission\":1168}")));

        client.commissions().estimate(CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                .unitPrice(Money.ofPln("100.00"))
                .build());

        // Strict JSON equality: any extra key — including an explicit "quantity":null — fails here.
        server.verify(postRequestedFor(urlEqualTo(ESTIMATE_PATH))
                .withRequestBody(equalToJson("{\"categoryId\":4,\"quantity\":1,\"unitPrice\":10000}", false, false)));
    }

    @Test
    void mapsNonLeafCategoryRejectionToValidationException() {
        // Real observed body: POST /commissions/_estimate with a non-leaf categoryId on the sandbox.
        String body = """
                {"name":"ValidationFailure","message":"Wystąpił problem z walidacją danych, \
                categoryId: invalid query for non-leaf category","failureType":"validation",\
                "polishMessage":"Problem z walidacją, sprawdź pola",\
                "payload":{"details":{"categoryId":"invalid query for non-leaf category"}},\
                "spanId":"lmtpsZ6QF4CBM","traceId":"lmtpsZ6QF4CBM"}""";
        server.stubFor(post(urlEqualTo(ESTIMATE_PATH))
                .willReturn(aResponse().withStatus(400).withBody(body)));

        ErliValidationException failure = assertThrows(ErliValidationException.class,
                () -> client.commissions().estimate(CommissionEstimateRequest.builder()
                        .categoryId(CategoryId.of("1"))
                        .unitPrice(Money.ofPln("100.00"))
                        .build()));

        assertEquals("lmtpsZ6QF4CBM", failure.details().traceId());
        assertEquals("validation", failure.details().failureType());
    }

    @Test
    void rejectsANonPlnUnitPriceBeforeTouchingTheWire() {
        assertThrows(IllegalArgumentException.class,
                () -> client.commissions().estimate(CommissionEstimateRequest.builder()
                        .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                        .unitPrice(Money.of("100.00", "EUR"))
                        .build()));

        server.verify(0, postRequestedFor(urlEqualTo(ESTIMATE_PATH)));
    }
}
