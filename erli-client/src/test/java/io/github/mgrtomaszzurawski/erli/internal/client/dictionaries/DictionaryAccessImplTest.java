package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryAccessImplTest {

    private static final String DELIVERY_METHODS_PATH = "/dictionaries/deliveryMethods";
    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String EXPECTED_AUTHORIZATION = "Bearer " + TEST_KEY;
    private static final String MEDIA_TYPE_JSON = "application/json";

    /** Verbatim from the live sandbox, 2026-07-25 (first of 114 entries). */
    private static final String OBSERVED_DELIVERY_METHODS_JSON =
            "[{\"id\":\"erliPaczkomat\",\"name\":\"ERLI InPost Paczkomaty 24/7\",\"cod\":false,\"vendor\":\"inpost\"}]";

    /**
     * Verbatim from the live sandbox, 2026-07-25: {@code POST /dictionaries/attributes/_search} with an
     * empty body. Richer than the spec — carries {@code payload.details} and {@code polishMessage}.
     */
    private static final String OBSERVED_400_BODY = """
            {"name":"ValidationFailure",\
            "message":"Wystąpił problem z walidacją danych, categoryId: categoryId must be a number",\
            "failureType":"validation","polishMessage":"Problem z walidacją, sprawdź pola",\
            "payload":{"details":{"categoryId":"\\"categoryId\\" must be a number"}},\
            "spanId":"699t9FoRLPuu1","traceId":"699t9FoRLPuu1"}""";

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

    private DictionaryAccessImpl dictionaries() {
        JsonCodec codec = new JsonCodec();
        HttpRuntime runtime = new HttpRuntime(
                HttpClient.newHttpClient(),
                server.baseUrl(),
                ApiKey.of(TEST_KEY),
                RetryPolicy.none(),
                USER_AGENT,
                Duration.ofSeconds(5),
                codec,
                new ErrorMapper(codec));
        return new DictionaryAccessImpl(runtime);
    }

    @Test
    void requestsDeliveryMethodsWithTheCredentialAndMapsTheResponse() {
        server.stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .willReturn(okJson(OBSERVED_DELIVERY_METHODS_JSON)));

        List<DeliveryMethod> methods = dictionaries().deliveryMethods();

        // Verify-on-write: assert the request we produced, not only the response we got back.
        server.verify(getRequestedFor(urlEqualTo(DELIVERY_METHODS_PATH))
                .withHeader("Authorization", equalTo(EXPECTED_AUTHORIZATION))
                .withHeader("User-Agent", equalTo(USER_AGENT))
                .withHeader("Accept", equalTo(MEDIA_TYPE_JSON)));

        assertEquals(1, methods.size());
        assertEquals(DeliveryMethodId.of("erliPaczkomat"), methods.get(0).id());
        assertEquals(DeliveryVendor.INPOST, methods.get(0).vendor());
    }

    @Test
    void mapsAnEmptyDictionaryToAnEmptyList() {
        server.stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH)).willReturn(okJson("[]")));

        assertTrue(dictionaries().deliveryMethods().isEmpty());
    }

    /** The mandatory per-bucket error-path table (TESTING.md): HTTP status to remediation exception. */
    @ParameterizedTest(name = "HTTP {0} maps to {1}")
    @CsvSource({
            "401, ErliAuthException",
            "403, ErliAuthException",
            "404, ErliNotFoundException",
            "400, ErliValidationException",
            "409, ErliValidationException",
            "422, ErliValidationException",
            "500, ErliServerException",
            "503, ErliServerException",
    })
    void mapsErrorStatusesToTheirRemediationException(int status, String expectedExceptionName) {
        server.stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .willReturn(aResponse().withStatus(status)
                        .withHeader("Content-Type", MEDIA_TYPE_JSON)
                        .withBody(OBSERVED_400_BODY)));

        ErliException failure = assertThrows(ErliException.class, () -> dictionaries().deliveryMethods());

        assertEquals(expectedExceptionName, failure.getClass().getSimpleName());
    }

    @Test
    void preservesTheRicherThanSpecErrorPayloadObservedLive() {
        server.stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", MEDIA_TYPE_JSON)
                        .withBody(OBSERVED_400_BODY)));

        ErliValidationException failure =
                assertThrows(ErliValidationException.class, () -> dictionaries().deliveryMethods());

        assertEquals("699t9FoRLPuu1", failure.details().traceId());
        assertEquals("699t9FoRLPuu1", failure.details().spanId());
        assertEquals("validation", failure.details().failureType());
        assertEquals("Problem z walidacją, sprawdź pola", failure.details().polishMessage());
        assertTrue(failure.details().rawBody().contains("categoryId"), failure.details().rawBody());
    }

    @Test
    void mapsANonJsonErrorBodyAndKeepsItRaw() {
        server.stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .willReturn(aResponse().withStatus(500).withBody("<html>gateway blew up</html>")));

        ErliException failure = assertThrows(ErliException.class, () -> dictionaries().deliveryMethods());

        assertInstanceOf(ErliServerException.class, failure);
    }
}
