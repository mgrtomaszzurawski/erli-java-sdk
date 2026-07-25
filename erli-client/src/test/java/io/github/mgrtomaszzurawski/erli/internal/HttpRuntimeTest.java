package io.github.mgrtomaszzurawski.erli.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpRuntimeTest {

    private static final String ME_PATH = "/me";
    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String SHOP_JSON =
            "{\"id\":100007,\"name\":\"test-shop\",\"active\":false,\"externalMatchingPolicy\":\"disabled\"}";
    private static final String OBSERVED_401_BODY =
            "{\"failureType\":\"security\",\"message\":\"Invalid API key\",\"httpCode\":401,\"spanId\":\"span-1\"}";

    private WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private HttpRuntime runtimeWith(RetryPolicy retryPolicy) {
        JsonCodec codec = new JsonCodec();
        return new HttpRuntime(
                HttpClient.newHttpClient(),
                server.baseUrl(),
                ApiKey.of(TEST_KEY),
                retryPolicy,
                USER_AGENT,
                Duration.ofSeconds(5),
                codec,
                new ErrorMapper(codec));
    }

    private static RetryPolicy fastRetry() {
        return RetryPolicy.builder()
                .maxAttempts(3)
                .baseDelay(Duration.ofMillis(1))
                .maxDelay(Duration.ofMillis(2))
                .randomGenerator(new Random(0))
                .build();
    }

    @Test
    void decodesSuccessBodyAndSendsBearerAndUserAgent() {
        server.stubFor(get(urlEqualTo(ME_PATH)).willReturn(okJson(SHOP_JSON)));

        ShopResponse shop = runtimeWith(fastRetry()).get(ME_PATH, ShopResponse.class);

        assertEquals(100007, shop.getId().intValue());
        assertEquals("test-shop", shop.getName());
        server.verify(getRequestedFor(urlEqualTo(ME_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void mapsErrorResponseToRemediationException() {
        server.stubFor(get(urlEqualTo(ME_PATH))
                .willReturn(aResponse().withStatus(401).withBody(OBSERVED_401_BODY)));

        ErliAuthException thrown = assertThrows(ErliAuthException.class,
                () -> runtimeWith(fastRetry()).get(ME_PATH, ShopResponse.class));
        assertEquals("security", thrown.details().failureType());
    }

    @Test
    void retriesServerErrorThenSucceeds() {
        String scenario = "retry-500";
        server.stubFor(get(urlEqualTo(ME_PATH)).inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("recovered"));
        server.stubFor(get(urlEqualTo(ME_PATH)).inScenario(scenario)
                .whenScenarioStateIs("recovered")
                .willReturn(okJson(SHOP_JSON)));

        ShopResponse shop = runtimeWith(fastRetry()).get(ME_PATH, ShopResponse.class);

        assertEquals(100007, shop.getId().intValue());
        server.verify(2, getRequestedFor(urlEqualTo(ME_PATH)));
    }

    @Test
    void doesNotRetryWhenPolicyForbidsIt() {
        server.stubFor(get(urlEqualTo(ME_PATH)).willReturn(aResponse().withStatus(503)));

        assertThrows(ErliServerException.class,
                () -> runtimeWith(RetryPolicy.none()).get(ME_PATH, ShopResponse.class));
        server.verify(1, getRequestedFor(urlEqualTo(ME_PATH)));
    }
}
