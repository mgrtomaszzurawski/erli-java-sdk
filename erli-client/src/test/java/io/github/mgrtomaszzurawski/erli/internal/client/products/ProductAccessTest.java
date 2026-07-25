package io.github.mgrtomaszzurawski.erli.internal.client.products;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.products.BatchUpdateOutcome;
import io.github.mgrtomaszzurawski.erli.domain.products.DispatchTime;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAccess;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductContent;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDraft;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFilter;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFilterField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductImage;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductPatch;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSearchRequest;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductUpdateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify-on-write coverage for the products facade: every test asserts the request the SDK produced, not
 * merely that a stub answered. A stub returning 200 for anything would pass a naive test even if the SDK
 * sent the wrong method, path, headers or body.
 *
 * <p>Includes the mandatory error-path table (TESTING.md): each HTTP status the bucket can surface is
 * mapped to its remediation exception.
 */
class ProductAccessTest {

    private static final String API_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String JSON_MEDIA_TYPE = "application/json";

    private static final String PRODUCT_PATH = "/products/sku-1";
    private static final String SEARCH_PATH = "/products/_search";
    private static final String BATCH_PATH = "/products/batch-update";
    private static final ProductExternalId SKU_1 = ProductExternalId.of("sku-1");

    private static final String OBSERVED_401_BODY =
            "{\"failureType\":\"security\",\"message\":\"Invalid API key\",\"httpCode\":401,"
                    + "\"polishMessage\":\"Nieprawidłowy klucz API\",\"spanId\":\"span-1\"}";

    private WireMockServer server;
    private ErliClient client;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = ErliClient.builder()
                .baseUrl(server.baseUrl())
                .apiKey(ApiKey.of(API_KEY))
                .userAgent(USER_AGENT)
                .retryPolicy(RetryPolicy.none())
                .build();
    }

    @AfterEach
    void stopServer() {
        client.close();
        server.stop();
    }

    private ProductAccess products() {
        return client.products();
    }

    private static ProductDraft minimalDraft() {
        return ProductDraft.of(ProductContent.builder()
                .name("Kurtka zimowa")
                .price(Money.ofPln("100.00"))
                .stock(10)
                .dispatchTime(DispatchTime.ofDays(1))
                .images(List.of(ProductImage.of("https://example.com/cover.jpg")))
                .build());
    }

    @Test
    void createSendsThePayloadErliExpectsAndAcceptsAnEmpty202() {
        server.stubFor(post(urlEqualTo(PRODUCT_PATH)).willReturn(aResponse().withStatus(202)));

        products().create(SKU_1, minimalDraft());

        server.verify(postRequestedFor(urlEqualTo(PRODUCT_PATH))
                .withHeader(AUTHORIZATION_HEADER, equalTo(BEARER_PREFIX + API_KEY))
                .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT))
                .withHeader(CONTENT_TYPE_HEADER, equalTo(JSON_MEDIA_TYPE))
                .withRequestBody(matchingJsonPath("$.name", equalTo("Kurtka zimowa")))
                // Prices go on the wire as integer grosze, never as a decimal.
                .withRequestBody(matchingJsonPath("$.price", equalTo("10000")))
                .withRequestBody(matchingJsonPath("$.stock", equalTo("10")))
                .withRequestBody(matchingJsonPath("$.dispatchTime.unit", equalTo("day")))
                .withRequestBody(matchingJsonPath("$.dispatchTime.period", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.images[0].url",
                        equalTo("https://example.com/cover.jpg"))));
    }

    @Test
    void percentEncodesASellerAssignedIdThatWouldOtherwiseChangeThePath() {
        server.stubFor(post(urlEqualTo("/products/a%2Fb%20c")).willReturn(aResponse().withStatus(202)));

        products().create(ProductExternalId.of("a/b c"), minimalDraft());

        // Unencoded this would have addressed /products/a/b c — a different resource entirely.
        server.verify(postRequestedFor(urlEqualTo("/products/a%2Fb%20c")));
    }

    @Test
    @Disabled("RED until the core JsonCodec fix lands (BACKLOG: the JsonNullableModule + NON_NULL half "
            + "of fix/core-jsoncodec-java-time). Today an untouched field serializes as an explicit null "
            + "and a JsonNullable one as {\"present\":false}, so this PATCH would ask Erli to wipe name, "
            + "price and dispatchTime. Re-enable — do not weaken — once that merges; the assertions here "
            + "are the contract.")
    void updateOmitsUntouchedFieldsAndSendsAnExplicitNullOnlyForClearedOnes() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH))
                .willReturn(okJson("{\"updatedFields\":[\"stock\",\"mobilePrice\"]}")));

        ProductUpdateResult result = products().update(SKU_1, ProductPatch.builder()
                .content(ProductContent.builder().stock(5).build())
                .clear(ProductField.MOBILE_PRICE)
                .build());

        server.verify(patchRequestedFor(urlEqualTo(PRODUCT_PATH))
                .withHeader(CONTENT_TYPE_HEADER, equalTo(JSON_MEDIA_TYPE))
                .withRequestBody(matchingJsonPath("$.stock", equalTo("5")))
                // Cleared: present and null. Untouched: absent. Conflating the two would wipe data.
                .withRequestBody(matchingJsonPath("$[?(@.mobilePrice == null)]"))
                .withRequestBody(matchingJsonPath("$[?(!@.name)]")));
        assertTrue(result.changed(ProductField.STOCK));
        assertTrue(result.changed(ProductField.MOBILE_PRICE));
        assertFalse(result.changed(ProductField.NAME));
    }

    @Test
    void keepsUnknownUpdatedFieldNamesInsteadOfFailingOnANewerApi() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH))
                .willReturn(okJson("{\"updatedFields\":[\"stock\",\"aFieldFromAFutureApi\"]}")));

        ProductUpdateResult result = products().update(SKU_1, ProductPatch.builder()
                .content(ProductContent.builder().stock(1).build())
                .build());

        assertTrue(result.changed(ProductField.STOCK));
        assertEquals(java.util.Set.of("aFieldFromAFutureApi"), result.unrecognisedFields());
    }

    @Test
    void batchUpdateReportsPerProductOutcomesRatherThanThrowingOnOneFailure() {
        server.stubFor(patch(urlEqualTo(BATCH_PATH)).willReturn(okJson(
                "[{\"externalId\":\"sku-1\",\"status\":202,\"result\":{\"updatedFields\":[\"stock\"]}},"
                        + "{\"externalId\":\"sku-2\",\"status\":408,\"error\":{\"name\":\"BatchProcessingTimeout\","
                        + "\"message\":\"Batch processing timeout exceeded\",\"traceId\":\"trace-9\"}}]")));

        List<BatchUpdateOutcome> outcomes = products().updateAll(Map.of(
                SKU_1, ProductPatch.builder().content(ProductContent.builder().stock(5).build()).build()));

        server.verify(patchRequestedFor(urlEqualTo(BATCH_PATH))
                .withRequestBody(matchingJsonPath("$[0].externalId", equalTo("sku-1")))
                .withRequestBody(matchingJsonPath("$[0].stock", equalTo("5"))));
        assertEquals(2, outcomes.size());
        assertTrue(outcomes.get(0).isAccepted());
        assertTrue(outcomes.get(0).result().orElseThrow().changed(ProductField.STOCK));
        assertFalse(outcomes.get(1).isAccepted());
        assertEquals("BatchProcessingTimeout", outcomes.get(1).error().orElseThrow().name());
        assertEquals("trace-9", outcomes.get(1).error().orElseThrow().traceId().orElseThrow().value());
    }

    @Test
    void anEmptyBatchIsANoOpAndSpendsNoRequest() {
        assertTrue(products().updateAll(Map.of()).isEmpty());
        assertEquals(0, server.getAllServeEvents().size());
    }

    @Test
    void searchSendsTheNestedFilterAndPaginationErliExpects() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        products().search(ProductSearchRequest.builder()
                .filter(ProductFilter.and(
                        ProductFilter.equalTo(ProductFilterField.STATUS, "active"),
                        ProductFilter.not(ProductFilter.equalTo(ProductFilterField.ARCHIVED, "true")),
                        ProductFilter.in(ProductFilterField.SKU, List.of("a", "b"))))
                .fields(java.util.Set.of(ProductField.NAME))
                .pageSize(25)
                .build()).count();

        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination.limit", equalTo("25")))
                .withRequestBody(matchingJsonPath("$.pagination.sortField", equalTo("externalId")))
                .withRequestBody(matchingJsonPath("$.pagination.order", equalTo("ASC")))
                .withRequestBody(matchingJsonPath("$.fields[0]", equalTo("name")))
                .withRequestBody(matchingJsonPath("$.filter.operator", equalTo("and")))
                .withRequestBody(matchingJsonPath("$.filter.value[0].field", equalTo("status")))
                // archived is a boolean on the wire, not the string "true".
                .withRequestBody(matchingJsonPath("$.filter.value[1].value.value", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.filter.value[2].operator", equalTo("in"))));
    }

    @Test
    void searchStopsWithoutASecondRequestWhenTheFirstPageIsEmpty() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        assertEquals(0, products().search(ProductSearchRequest.all()).count());

        server.verify(1, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void getReturnsEmptyRatherThanThrowingWhenNoSuchProductExists() {
        server.stubFor(get(urlEqualTo(PRODUCT_PATH)).willReturn(aResponse()
                .withStatus(404)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody("{\"message\":\"Product not found\"}")));

        assertTrue(products().get(SKU_1).isEmpty());

        server.verify(getRequestedFor(urlEqualTo(PRODUCT_PATH))
                .withHeader(AUTHORIZATION_HEADER, equalTo(BEARER_PREFIX + API_KEY)));
    }

    @Test
    void getSendsTheFieldProjectionAsACommaJoinedQueryParameter() {
        String projected = "/products/sku-1?fields=name,price";
        server.stubFor(get(urlEqualTo(projected)).willReturn(aResponse()
                .withStatus(404)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody("{\"message\":\"Product not found\"}")));

        products().get(SKU_1, new java.util.LinkedHashSet<>(
                List.of(ProductField.NAME, ProductField.PRICE)));

        server.verify(getRequestedFor(urlEqualTo(projected)));
    }

    @Test
    void getDiscountReturnsEmptyWhenTheProductHasNoPromotion() {
        server.stubFor(get(urlEqualTo("/products/sku-1/discount")).willReturn(aResponse()
                .withStatus(404)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody("{\"message\":\"No discount\"}")));

        assertTrue(products().getDiscount(SKU_1).isEmpty());

        server.verify(getRequestedFor(urlEqualTo("/products/sku-1/discount")));
    }

    // --- The mandatory error-path table (TESTING.md) ------------------------------------------------

    @Test
    void mapsAnObservedUnauthorizedBodyToTheAuthException() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH)).willReturn(aResponse()
                .withStatus(401)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody(OBSERVED_401_BODY)));

        assertThrows(ErliAuthException.class, () -> products().update(SKU_1, emptyPatch()));
    }

    @Test
    void mapsNotFoundToTheNotFoundExceptionWhenTheCallerDidNotAskAboutExistence() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH)).willReturn(aResponse()
                .withStatus(404)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody("{\"message\":\"Product not found\"}")));

        assertThrows(ErliNotFoundException.class, () -> products().update(SKU_1, emptyPatch()));
    }

    @Test
    void mapsAConflictOnCreateToTheValidationException() {
        server.stubFor(post(urlEqualTo(PRODUCT_PATH)).willReturn(aResponse()
                .withStatus(409)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody("{\"message\":\"Product already exists\"}")));

        assertThrows(ErliValidationException.class, () -> products().create(SKU_1, minimalDraft()));
    }

    @Test
    void mapsAServerErrorToTheServerException() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(aResponse()
                .withStatus(500)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody("{\"message\":\"Internal error\"}")));

        assertThrows(ErliServerException.class,
                () -> products().search(ProductSearchRequest.all()).count());
    }

    @Test
    void preservesANonJsonErrorBodyInsteadOfFailingToParseIt() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(aResponse()
                .withStatus(503)
                .withHeader(CONTENT_TYPE_HEADER, "text/html")
                .withBody("<html><body>Service unavailable</body></html>")));

        ErliServerException failure = assertThrows(ErliServerException.class,
                () -> products().search(ProductSearchRequest.all()).count());
        assertTrue(failure.details().rawBody().contains("Service unavailable"), failure.getMessage());
    }

    private static ProductPatch emptyPatch() {
        return ProductPatch.builder().content(ProductContent.builder().stock(1).build()).build();
    }

    @Test
    void rejectsUseAfterTheClientIsClosed() {
        client.close();
        assertThrows(IllegalStateException.class, this::products);
    }

    @Test
    void doesNotRetryANonIdempotentWriteThatFailedWithAServerError() {
        server.stubFor(post(urlEqualTo(PRODUCT_PATH)).willReturn(aResponse()
                .withStatus(500)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody("{\"message\":\"Internal error\"}")));

        assertThrows(ErliServerException.class, () -> products().create(SKU_1, minimalDraft()));

        // A create is not idempotent: retrying could publish the product twice.
        server.verify(1, postRequestedFor(urlEqualTo(PRODUCT_PATH)));
    }

    @Test
    void sendsAnEqualToJsonBodyForASingleFieldPatch() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH)).willReturn(okJson("{\"updatedFields\":[\"stock\"]}")));

        products().update(SKU_1, emptyPatch());

        server.verify(patchRequestedFor(urlEqualTo(PRODUCT_PATH))
                .withRequestBody(equalToJson("{\"stock\":1}", true, true)));
    }
}
