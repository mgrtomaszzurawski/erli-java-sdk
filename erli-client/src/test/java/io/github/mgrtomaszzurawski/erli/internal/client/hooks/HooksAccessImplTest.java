package io.github.mgrtomaszzurawski.erli.internal.client.hooks;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.hooks.BuyabilityQuery;
import io.github.mgrtomaszzurawski.erli.domain.hooks.BuyabilityStatus;
import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HookKind;
import io.github.mgrtomaszzurawski.erli.domain.hooks.HooksAccess;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductBuyability;
import io.github.mgrtomaszzurawski.erli.domain.hooks.ProductSyncNotification;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify-on-write coverage of the five {@code /hooks} operations, plus the bucket's mandatory
 * error-path table ({@code TESTING.md}). Each test asserts the request the SDK produced — method, path,
 * Bearer credential and body — not only the response it decoded.
 */
class HooksAccessImplTest {

    private static final String HOOKS_PATH = "/hooks";
    private static final String HOOK_PATH = "/hooks/checkBuyability";
    private static final String BUYABILITY_RUN_PATH = "/hooks/checkBuyability/run";
    private static final String SYNC_RUN_PATH = "/hooks/productsNeedSync/run";
    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String HOOK_URL = "https://shop.example/check-buyability";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIs";

    private static final String HOOKS_JSON = """
            [{"hookName":"checkBuyability","url":"https://shop.example/check-buyability",
              "accessToken":"eyJhbGciOiJIUzI1NiIs"},
             {"hookName":"orderCreated","url":"https://shop.example/order-created"}]""";
    private static final String BUYABILITY_JSON = """
            [{"productId":"555","status":"active","stock":100},
             {"productId":"556","status":null}]""";
    private static final String OBSERVED_401_BODY =
            "{\"failureType\":\"security\",\"message\":\"Invalid API key\",\"httpCode\":401,\"spanId\":\"span-1\"}";
    private static final String VALIDATION_BODY =
            "{\"failureType\":\"failure\",\"message\":\"url is required\",\"httpCode\":400}";

    private static final int HTTP_CREATED = 201;
    private static final int HTTP_ACCEPTED = 202;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_SERVER_ERROR = 500;
    private static final int MAX_ATTEMPTS = 3;
    private static final int EXPECTED_HOOK_COUNT = 2;
    private static final int EXPECTED_STOCK = 100;
    private static final int BUYABILITY_QUANTITY = 2;

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

    private HooksAccess hooks() {
        JsonCodec codec = new JsonCodec();
        HttpRuntime runtime = new HttpRuntime(
                HttpClient.newHttpClient(),
                server.baseUrl(),
                ApiKey.of(TEST_KEY),
                RetryPolicy.builder()
                        .maxAttempts(MAX_ATTEMPTS)
                        .baseDelay(Duration.ofMillis(1))
                        .maxDelay(Duration.ofMillis(2))
                        .randomGenerator(new Random(0))
                        .build(),
                USER_AGENT,
                Duration.ofSeconds(5),
                codec,
                new ErrorMapper(codec));
        return new HooksAccessImpl(runtime);
    }

    @Test
    void listsRegisteredHooks() {
        server.stubFor(get(urlEqualTo(HOOKS_PATH)).willReturn(okJson(HOOKS_JSON)));

        List<Hook> registered = hooks().list();

        assertEquals(EXPECTED_HOOK_COUNT, registered.size());
        assertEquals(HookKind.CHECK_BUYABILITY, registered.get(0).kind());
        assertEquals(URI.create(HOOK_URL), registered.get(0).url());
        assertEquals(ACCESS_TOKEN, registered.get(0).accessToken().orElseThrow());
        assertEquals(HookKind.ORDER_CREATED, registered.get(1).kind());
        assertTrue(registered.get(1).accessToken().isEmpty());
        server.verify(getRequestedFor(urlEqualTo(HOOKS_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void treatsAnEmptyHookListAsNoSubscriptions() {
        server.stubFor(get(urlEqualTo(HOOKS_PATH)).willReturn(okJson("[]")));

        assertEquals(List.of(), hooks().list());
    }

    @Test
    void savesAHookToThePathNamedByItsKind() {
        server.stubFor(put(urlEqualTo(HOOK_PATH)).willReturn(aResponse().withStatus(HTTP_CREATED)));

        hooks().save(Hook.of(HookKind.CHECK_BUYABILITY, URI.create(HOOK_URL), ACCESS_TOKEN));

        server.verify(putRequestedFor(urlEqualTo(HOOK_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("{\"hookName\":\"checkBuyability\",\"url\":\"" + HOOK_URL
                        + "\",\"accessToken\":\"" + ACCESS_TOKEN + "\"}")));
    }

    /**
     * An absent access token must be left out of the body rather than sent as {@code "accessToken":null}
     * — the request schema is {@code additionalProperties:false} and a null would be a value to store.
     */
    @Test
    void omitsAnAbsentAccessTokenFromTheSaveBody() {
        server.stubFor(put(urlEqualTo(HOOK_PATH)).willReturn(aResponse().withStatus(HTTP_CREATED)));

        hooks().save(Hook.of(HookKind.CHECK_BUYABILITY, URI.create(HOOK_URL)));

        server.verify(putRequestedFor(urlEqualTo(HOOK_PATH)).withRequestBody(
                equalToJson("{\"hookName\":\"checkBuyability\",\"url\":\"" + HOOK_URL + "\"}")));
    }

    /** The https rule is enforced where it matters: nothing insecure may reach the API. */
    @Test
    void refusesToSaveAnInsecureHookWithoutCallingTheApi() {
        Hook insecure = new Hook(
                HookKind.ORDER_CREATED, URI.create("http://legacy.example/hook"), Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> hooks().save(insecure));

        server.verify(0, putRequestedFor(urlEqualTo("/hooks/orderCreated")));
    }

    @Test
    void deletesAHookByKind() {
        server.stubFor(delete(urlEqualTo(HOOK_PATH)).willReturn(aResponse().withStatus(HTTP_ACCEPTED)));

        hooks().delete(HookKind.CHECK_BUYABILITY);

        server.verify(deleteRequestedFor(urlEqualTo(HOOK_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY)));
    }

    @Test
    void testFiresBuyabilityAndMapsTheNullableStatus() {
        server.stubFor(post(urlEqualTo(BUYABILITY_RUN_PATH)).willReturn(okJson(BUYABILITY_JSON)));

        List<ProductBuyability> answers = hooks().checkBuyability(List.of(
                BuyabilityQuery.of(ProductExternalId.of("555"), BUYABILITY_QUANTITY),
                BuyabilityQuery.of(ProductExternalId.of("556"), 1)));

        assertEquals(BuyabilityStatus.ACTIVE, answers.get(0).status().orElseThrow());
        assertEquals(EXPECTED_STOCK, answers.get(0).stock().orElseThrow());
        // The API declares `status` explicitly nullable; an explicit null must read as "not stated".
        assertTrue(answers.get(1).status().isEmpty());
        assertTrue(answers.get(1).stock().isEmpty());
        server.verify(postRequestedFor(urlEqualTo(BUYABILITY_RUN_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(
                        "[{\"productId\":\"555\",\"quantity\":2},{\"productId\":\"556\",\"quantity\":1}]")));
    }

    @Test
    void testFiresProductSyncWithFieldsWhenNamed() {
        server.stubFor(post(urlEqualTo(SYNC_RUN_PATH)).willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        hooks().notifyProductsNeedSync(ProductSyncNotification.ofFields(
                List.of(ProductExternalId.of("123"), ProductExternalId.of("456")), List.of("price")));

        server.verify(postRequestedFor(urlEqualTo(SYNC_RUN_PATH)).withRequestBody(equalToJson(
                "{\"externalProductIds\":[\"123\",\"456\"],\"fields\":[\"price\"]}")));
    }

    @Test
    void omitsFieldsWhenTheWholeProductNeedsSyncing() {
        server.stubFor(post(urlEqualTo(SYNC_RUN_PATH)).willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        hooks().notifyProductsNeedSync(
                ProductSyncNotification.ofProducts(List.of(ProductExternalId.of("123"))));

        server.verify(postRequestedFor(urlEqualTo(SYNC_RUN_PATH))
                .withRequestBody(equalToJson("{\"externalProductIds\":[\"123\"]}")));
    }

    // --- mandatory error-path table (TESTING.md) ---------------------------------------------------

    @Test
    void mapsUnauthorizedToTheAuthRemediation() {
        server.stubFor(get(urlEqualTo(HOOKS_PATH))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody(OBSERVED_401_BODY)));

        ErliAuthException thrown = assertThrows(ErliAuthException.class, () -> hooks().list());

        assertEquals("security", thrown.details().failureType());
    }

    @Test
    void mapsNotFoundToTheNotFoundRemediation() {
        server.stubFor(delete(urlEqualTo(HOOK_PATH))
                .willReturn(aResponse().withStatus(HTTP_NOT_FOUND).withBody("{\"message\":\"no such hook\"}")));

        assertThrows(ErliNotFoundException.class, () -> hooks().delete(HookKind.CHECK_BUYABILITY));
    }

    @Test
    void mapsBadRequestToTheValidationRemediation() {
        server.stubFor(put(urlEqualTo(HOOK_PATH))
                .willReturn(aResponse().withStatus(HTTP_BAD_REQUEST).withBody(VALIDATION_BODY)));

        assertThrows(ErliValidationException.class,
                () -> hooks().save(Hook.of(HookKind.CHECK_BUYABILITY, URI.create(HOOK_URL))));
    }

    @Test
    void mapsServerErrorToTheServerRemediation() {
        server.stubFor(post(urlEqualTo(SYNC_RUN_PATH))
                .willReturn(aResponse().withStatus(HTTP_SERVER_ERROR)));

        assertThrows(ErliServerException.class, () -> hooks().notifyProductsNeedSync(
                ProductSyncNotification.ofProducts(List.of(ProductExternalId.of("123")))));
    }
}
