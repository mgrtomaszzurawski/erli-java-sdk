package io.github.mgrtomaszzurawski.erli.internal.client.delivery;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryMethodRef;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryPrice;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryTime;
import io.github.mgrtomaszzurawski.erli.domain.delivery.DeliveryTimeUnit;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PackingLimit;
import io.github.mgrtomaszzurawski.erli.domain.delivery.ParcelSize;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceList;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListDraft;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListQuery;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListSummary;
import io.github.mgrtomaszzurawski.erli.domain.delivery.PriceListUpdate;
import io.github.mgrtomaszzurawski.erli.domain.delivery.SizeLimit;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Drives the delivery facade against a real local HTTP server. Every write case verifies the request
 * the SDK <em>produced</em> — a stub answering anything would hide a wrong verb, a mis-templated path
 * or, here most importantly, an amount sent in the wrong scale.
 */
class DeliveryAccessImplTest {

    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String PRICE_LISTS_PATH = "/delivery/priceLists";
    private static final String PRICE_LISTS_DETAILS_PATH = "/delivery/priceListsDetails";
    private static final String PRICE_LIST_PATH = "/delivery/priceList";
    private static final String PRICE_LIST_BY_ID_PATH = "/delivery/priceList/42";
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;
    private static final int HTTP_SERVER_ERROR = 500;

    private static final String SUMMARIES_JSON = """
            [ { "id": 42, "name": "*" }, { "id": 43, "name": "heavy" } ]
            """;
    private static final String DETAILS_JSON = """
            [ {
              "id": 42,
              "name": "*",
              "erliProEnabled": true,
              "nextDayDeliveryEnabled": false,
              "createdAt": "2026-07-20T08:14:00Z",
              "updatedAt": "2026-07-21T09:30:00Z",
              "prices": [ {
                "deliveryMethod": {
                  "id": "erliPaczkomat",
                  "deliveryTime": { "unit": "days", "minPeriod": 1, "maxPeriod": 2 }
                },
                "basePrice": 1049,
                "nextItemPrice": 800,
                "limit": [
                  { "dimension": "A", "limit": 3 },
                  { "dimension": "B", "limit": 2 },
                  { "dimension": "C", "limit": 1 }
                ],
                "nextDayDeliveryOption": true
              }, {
                "deliveryMethod": { "id": "erliDHL5kg" },
                "basePrice": 1500,
                "nextItemPrice": 0,
                "limit": 10
              } ]
            } ]
            """;
    private static final String CREATED_JSON = """
            { "id": 42, "name": "heavy", "createdAt": "2026-07-20T08:14:00Z",
              "prices": [ { "deliveryMethod": { "id": "erliDHL5kg" }, "basePrice": 1500, "nextItemPrice": 0 } ] }
            """;

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

    private DeliveryAccessImpl deliveryAccess() {
        JsonCodec codec = new JsonCodec();
        HttpRuntime runtime = new HttpRuntime(HttpClient.newHttpClient(), server.baseUrl(), ApiKey.of(TEST_KEY),
                RetryPolicy.builder().maxAttempts(2).baseDelay(Duration.ofMillis(1))
                        .maxDelay(Duration.ofMillis(2)).randomGenerator(new Random(0)).build(),
                USER_AGENT, Duration.ofSeconds(5), codec, new ErrorMapper(codec));
        return new DeliveryAccessImpl(runtime);
    }

    private static DeliveryPrice samplePrice() {
        return new DeliveryPrice(
                new DeliveryMethodRef(DeliveryMethodId.of("erliDHL5kg"),
                        Optional.of(new DeliveryTime(DeliveryTimeUnit.DAYS, 1, 2))),
                Money.ofMinorUnits(1500, "PLN"),
                Money.ofMinorUnits(0, "PLN"),
                Optional.of(new PackingLimit.Total(10)),
                false);
    }

    @Test
    void listsPriceListSummaries() {
        server.stubFor(get(urlEqualTo(PRICE_LISTS_PATH)).willReturn(okJson(SUMMARIES_JSON)));

        List<PriceListSummary> summaries = deliveryAccess().priceLists();

        assertEquals(2, summaries.size());
        assertEquals(42L, summaries.get(0).id());
        assertEquals("*", summaries.get(0).name());
        server.verify(getRequestedFor(urlEqualTo(PRICE_LISTS_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void mapsGroszeToMoneyAndRetypesBothPackingLimitBranches() {
        server.stubFor(get(urlPathEqualTo(PRICE_LISTS_DETAILS_PATH)).willReturn(okJson(DETAILS_JSON)));

        PriceList priceList = deliveryAccess().priceListDetails(PriceListQuery.none()).get(0);

        assertEquals(42L, priceList.id());
        assertTrue(priceList.erliProEnabled());
        assertFalse(priceList.nextDayDeliveryEnabled());
        assertEquals(Money.ofMinorUnits(1049, "PLN"), priceList.prices().get(0).basePrice());
        assertEquals(Money.ofMinorUnits(800, "PLN"), priceList.prices().get(0).nextItemPrice());
        assertEquals(new DeliveryTime(DeliveryTimeUnit.DAYS, 1, 2),
                priceList.prices().get(0).deliveryMethod().deliveryTime().orElseThrow());

        PackingLimit perSize = priceList.prices().get(0).limit().orElseThrow();
        assertEquals(new PackingLimit.PerSize(List.of(new SizeLimit(ParcelSize.A, 3),
                new SizeLimit(ParcelSize.B, 2), new SizeLimit(ParcelSize.C, 1))), perSize);
        assertEquals(new PackingLimit.Total(10), priceList.prices().get(1).limit().orElseThrow());
    }

    @Test
    void sendsRepeatedFiltersAsSeparateQueryParameters() {
        server.stubFor(get(urlPathEqualTo(PRICE_LISTS_DETAILS_PATH)).willReturn(okJson("[]")));

        deliveryAccess().priceListDetails(
                PriceListQuery.builder().id(42).id(43).name("heavy").erliProEnabled(true).build());

        server.verify(getRequestedFor(urlPathEqualTo(PRICE_LISTS_DETAILS_PATH))
                .withQueryParam("id", equalTo("42"))
                .withQueryParam("name", equalTo("heavy"))
                .withQueryParam("erliProEnabled", equalTo("true")));
    }

    @Test
    void postsTheCreateBodyWithAmountsBackInGrosze() {
        server.stubFor(post(urlEqualTo(PRICE_LIST_PATH)).willReturn(okJson(CREATED_JSON)));

        PriceList created = deliveryAccess().createPriceList(
                PriceListDraft.builder("heavy").price(samplePrice()).erliProEnabled(true).build());

        assertEquals("heavy", created.name());
        server.verify(postRequestedFor(urlEqualTo(PRICE_LIST_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        { "name": "heavy", "erliProEnabled": true, "nextDayDeliveryEnabled": false,
                          "prices": [ { "deliveryMethod": { "id": "erliDHL5kg",
                              "deliveryTime": { "unit": "days", "minPeriod": 1, "maxPeriod": 2 } },
                            "basePrice": 1500, "nextItemPrice": 0, "limit": 10,
                            "nextDayDeliveryOption": false } ] }
                        """, true, true)));
    }

    @Test
    void patchesAPriceListOnItsTemplatedPath() {
        server.stubFor(patch(urlEqualTo(PRICE_LIST_BY_ID_PATH)).willReturn(okJson(CREATED_JSON)));

        deliveryAccess().updatePriceList(42L, PriceListUpdate.builder().price(samplePrice()).build());

        server.verify(patchRequestedFor(urlEqualTo(PRICE_LIST_BY_ID_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY)));
    }

    @Test
    void rejectsAnAmountThatIsNotAWholeNumberOfGroszeSeparatelyFromOneOutOfRange() {
        DeliveryPrice fractional = priceOf(new BigDecimal("10.499"));
        DeliveryPrice huge = priceOf(new BigDecimal("99999999999"));

        IllegalArgumentException notWhole = assertThrows(IllegalArgumentException.class,
                () -> deliveryAccess().createPriceList(PriceListDraft.builder("x").price(fractional).build()));
        IllegalArgumentException outOfRange = assertThrows(IllegalArgumentException.class,
                () -> deliveryAccess().createPriceList(PriceListDraft.builder("x").price(huge).build()));

        assertTrue(notWhole.getMessage().contains("whole number"), notWhole.getMessage());
        assertTrue(outOfRange.getMessage().contains("range"), outOfRange.getMessage());
        server.verify(0, postRequestedFor(urlEqualTo(PRICE_LIST_PATH)));
    }

    @Test
    void rejectsAnAmountInACurrencyTheDeliveryEndpointDoesNotPriceIn() {
        DeliveryPrice inYen = new DeliveryPrice(
                new DeliveryMethodRef(DeliveryMethodId.of("erliDHL5kg"), Optional.empty()),
                Money.ofMinorUnits(1299, "JPY"), Money.ofMinorUnits(0, "PLN"), Optional.empty(), false);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> deliveryAccess().createPriceList(PriceListDraft.builder("x").price(inYen).build()));

        assertTrue(failure.getMessage().contains("PLN"), failure.getMessage());
        server.verify(0, postRequestedFor(urlEqualTo(PRICE_LIST_PATH)));
    }

    private static DeliveryPrice priceOf(BigDecimal amount) {
        return new DeliveryPrice(
                new DeliveryMethodRef(DeliveryMethodId.of("erliDHL5kg"), Optional.empty()),
                Money.of(amount, java.util.Currency.getInstance("PLN")), Money.ofMinorUnits(0, "PLN"), Optional.
                        empty(), false);
    }

    @Test
    void rejectsADeliveryMethodTheVendoredSpecDoesNotKnowBeforeSendingAnything() {
        DeliveryPrice unknownMethod = new DeliveryPrice(
                new DeliveryMethodRef(DeliveryMethodId.of("erliTeleportation"), Optional.empty()),
                Money.ofMinorUnits(100, "PLN"), Money.ofMinorUnits(0, "PLN"), Optional.empty(), false);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> deliveryAccess().createPriceList(
                        PriceListDraft.builder("x").price(unknownMethod).build()));

        assertTrue(failure.getMessage().contains("erliTeleportation"), failure.getMessage());
        server.verify(0, postRequestedFor(urlEqualTo(PRICE_LIST_PATH)));
    }

    @Test
    void mapsAWriteEndpointErrorToItsRemediationExceptionToo() {
        server.stubFor(post(urlEqualTo(PRICE_LIST_PATH)).willReturn(aResponse().withStatus(HTTP_CONFLICT)
                .withBody("{\"errorCode\":1200,\"errorMessage\":\"name taken\"}")));

        // The conflict this area actually produces is on create, not on the read the table drives.
        assertThrows(ErliValidationException.class, () -> deliveryAccess().createPriceList(
                PriceListDraft.builder("*").price(samplePrice()).build()));
    }

    /** The mandatory error-path table ({@code TESTING.md}) for this area's read endpoint. */
    static Stream<Arguments> errorPathTable() {
        return Stream.of(
                Arguments.of(HTTP_UNAUTHORIZED,
                        "{\"failureType\":\"security\",\"message\":\"Invalid API key\",\"httpCode\":401}",
                        ErliAuthException.class),
                Arguments.of(HTTP_FORBIDDEN, "{\"failureType\":\"security\",\"httpCode\":403}",
                        ErliAuthException.class),
                Arguments.of(HTTP_NOT_FOUND, "{\"errorCode\":1400}", ErliNotFoundException.class),
                Arguments.of(HTTP_BAD_REQUEST, "{\"errorCode\":1200}", ErliValidationException.class),
                Arguments.of(HTTP_CONFLICT, "{\"errorCode\":1200,\"errorMessage\":\"name taken\"}",
                        ErliValidationException.class),
                Arguments.of(HTTP_SERVER_ERROR, "{\"errorCode\":1100}", ErliServerException.class));
    }

    @ParameterizedTest(name = "HTTP {0} maps to {2}")
    @MethodSource("errorPathTable")
    void mapsEveryErrorStatusToItsRemediationException(
            int status, String body, Class<? extends ErliException> expected) {
        server.stubFor(get(urlEqualTo(PRICE_LISTS_PATH))
                .willReturn(aResponse().withStatus(status).withBody(body)));

        assertThrows(expected, () -> deliveryAccess().priceLists());
    }
}
