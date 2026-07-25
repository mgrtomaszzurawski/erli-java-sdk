package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.shipping.Parcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatus;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Random;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Drives the shipping facade against a real local HTTP server. Every case verifies the request the
 * SDK <em>produced</em> (method, path, credential, headers), not only the response it decoded — a stub
 * that answers anything would otherwise hide a wrong verb or a mis-templated path.
 */
class ShippingAccessImplTest {

    private static final String PARCEL_ID = "55123";
    private static final String PARCEL_PATH = "/shipping/parcels/" + PARCEL_ID;
    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String MEDIA_TYPE_JSON = "application/json";

    /** The live 401 observed on the sandbox — richer than the spec (failureType/httpCode, no traceId). */
    private static final String OBSERVED_401_BODY =
            "{\"failureType\":\"security\",\"message\":\"Invalid API key\",\"httpCode\":401,\"spanId\":\"span-1\"}";
    private static final String NOT_FOUND_BODY =
            "{\"errorCode\":1402,\"errorMessage\":\"Przesylka o danym id nieodnaleziona\"}";
    private static final String VALIDATION_BODY =
            "{\"errorCode\":1201,\"errorMessage\":\"Blad walidacji przesylki\"}";
    private static final String SERVER_ERROR_BODY = "{\"errorCode\":1100,\"errorMessage\":\"Blad serwera\"}";

    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_SERVER_ERROR = 500;

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

    private ShippingAccessImpl shippingAccess() {
        JsonCodec codec = new JsonCodec();
        HttpRuntime runtime = new HttpRuntime(
                HttpClient.newHttpClient(),
                server.baseUrl(),
                ApiKey.of(TEST_KEY),
                fastRetry(),
                USER_AGENT,
                Duration.ofSeconds(5),
                codec,
                new ErrorMapper(codec));
        return new ShippingAccessImpl(runtime);
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
    void getsAParcelByIdAndSendsTheCredentialOnTheTemplatedPath() {
        server.stubFor(get(urlEqualTo(PARCEL_PATH)).willReturn(okJson(ParcelFixtures.FULL_PARCEL_JSON)));

        Parcel parcel = shippingAccess().parcel(ParcelId.of(PARCEL_ID));

        assertEquals(ParcelStatus.ON_THE_WAY, parcel.status());
        assertEquals(PARCEL_ID, parcel.id().orElseThrow().value());
        server.verify(getRequestedFor(urlEqualTo(PARCEL_PATH))
                .withHeader(AUTHORIZATION_HEADER, equalTo("Bearer " + TEST_KEY))
                .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT))
                .withHeader(ACCEPT_HEADER, equalTo(MEDIA_TYPE_JSON)));
    }

    @Test
    void percentEncodesTheIdSoItCannotEscapeItsPathSegment() {
        String hostileId = "55123/../me";
        String encodedPath = "/shipping/parcels/55123%2F..%2Fme";
        server.stubFor(get(urlEqualTo(encodedPath)).willReturn(okJson(ParcelFixtures.MINIMAL_PARCEL_JSON)));

        shippingAccess().parcel(ParcelId.of(hostileId));

        server.verify(getRequestedFor(urlEqualTo(encodedPath)));
    }

    /**
     * The mandatory error-path table ({@code TESTING.md}): every status this operation can surface maps
     * to its remediation exception. The 401 row runs against the body actually observed on the sandbox.
     */
    static Stream<Arguments> errorPathTable() {
        return Stream.of(
                Arguments.of(HTTP_UNAUTHORIZED, OBSERVED_401_BODY, ErliAuthException.class),
                Arguments.of(HTTP_NOT_FOUND, NOT_FOUND_BODY, ErliNotFoundException.class),
                Arguments.of(HTTP_BAD_REQUEST, VALIDATION_BODY, ErliValidationException.class),
                Arguments.of(HTTP_SERVER_ERROR, SERVER_ERROR_BODY, ErliServerException.class));
    }

    @ParameterizedTest(name = "HTTP {0} maps to {2}")
    @MethodSource("errorPathTable")
    void mapsEveryErrorStatusToItsRemediationException(
            int status, String body, Class<? extends ErliException> expected) {
        server.stubFor(get(urlEqualTo(PARCEL_PATH))
                .willReturn(aResponse().withStatus(status).withBody(body)));

        assertThrows(expected, () -> shippingAccess().parcel(ParcelId.of(PARCEL_ID)));
    }

    @Test
    void preservesANonJsonErrorBodyInsteadOfFailingToDecodeIt() {
        server.stubFor(get(urlEqualTo(PARCEL_PATH))
                .willReturn(aResponse().withStatus(HTTP_NOT_FOUND).withBody("<html>not found</html>")));

        ErliNotFoundException failure = assertThrows(
                ErliNotFoundException.class, () -> shippingAccess().parcel(ParcelId.of(PARCEL_ID)));

        assertEquals("<html>not found</html>", failure.details().rawBody());
    }

    @Test
    void retriesTheIdempotentReadOnceAfterAServerError() {
        String scenario = "parcel-retry";
        String secondAttempt = "second-attempt";
        server.stubFor(get(urlEqualTo(PARCEL_PATH)).inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR).withBody(SERVER_ERROR_BODY))
                .willSetStateTo(secondAttempt));
        server.stubFor(get(urlEqualTo(PARCEL_PATH)).inScenario(scenario)
                .whenScenarioStateIs(secondAttempt)
                .willReturn(okJson(ParcelFixtures.MINIMAL_PARCEL_JSON)));

        Parcel parcel = shippingAccess().parcel(ParcelId.of(PARCEL_ID));

        assertEquals(ParcelStatus.PREPARING, parcel.status());
        server.verify(2, getRequestedFor(urlEqualTo(PARCEL_PATH)));
    }
}
