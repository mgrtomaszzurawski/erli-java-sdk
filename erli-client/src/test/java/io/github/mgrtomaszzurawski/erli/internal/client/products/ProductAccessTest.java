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
import io.github.mgrtomaszzurawski.erli.domain.products.Discount;
import io.github.mgrtomaszzurawski.erli.domain.products.DiscountRequest;
import io.github.mgrtomaszzurawski.erli.domain.products.DispatchTime;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAccess;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductContent;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDraft;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFilter;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFilterField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductImage;
import io.github.mgrtomaszzurawski.erli.domain.products.Product;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductPatch;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSearchRequest;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSortField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductUpdateResult;
import io.github.mgrtomaszzurawski.erli.domain.products.SortOrder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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

    private static final String DISCOUNT_PATH = "/products/sku-1/discount";

    /** The smallest body ProductMapper can turn into a Product — what a widened projection returns. */
    private static final String MINIMAL_PRODUCT_BODY = productBody("sku-1");

    /** A fuller body, used where the test cares about the request rather than the mapping. */
    private static final String FULL_PRODUCT_BODY = productBody("sku-1");

    private static final String DISCOUNT_BODY =
            "{\"externalId\":\"sku-1\",\"shopId\":100007,\"newPrice\":7900,"
                    + "\"startAt\":\"2026-08-01T00:00:00+02:00\","
                    + "\"restoreAt\":\"2026-08-08T00:00:00+02:00\",\"unfreezeAfterwards\":true}";

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

    /** A response carrying exactly the fields a Product requires, and nothing else. */
    private static String productBody(String externalId) {
        return "{\"externalId\":\"" + externalId + "\",\"marketplaceId\":987654,"
                + "\"name\":\"Kurtka\",\"slug\":\"kurtka\",\"status\":\"active\",\"stock\":10,"
                + "\"price\":10000,\"dispatchTime\":{\"unit\":\"day\",\"period\":1},"
                + "\"frozen\":{},\"created\":\"2026-07-24T13:50:23.961+02:00\","
                // `updated` is present so a walk sorted by it can actually derive a cursor; without it
                // the stream would stop after page one for a different reason and the paging guard below
                // would never be reached.
                + "\"updated\":\"2026-07-25T09:00:00.000+02:00\"}";
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
    void neverSendsOverrideFrozenOnAnOrdinaryPatch() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH)).willReturn(okJson("{\"updatedFields\":[\"stock\"]}")));

        products().update(SKU_1, stockPatch());

        // Layer 1 declares overrideFrozen as an explicit null (JsonNullable.of(null)) rather than
        // undefined, so leaving it alone puts "overrideFrozen": null on every update and the marketplace
        // answers 400 "overrideFrozen must be [true]". Observed live, 2026-07-25.
        server.verify(patchRequestedFor(urlEqualTo(PRODUCT_PATH))
                .withRequestBody(equalToJson("{\"stock\":1}", true, true)));
    }

    @Test
    void sendsOverrideFrozenOnlyWhenTheCallerAsksForIt() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH)).willReturn(okJson("{\"updatedFields\":[\"stock\"]}")));

        products().update(SKU_1, ProductPatch.builder()
                .content(ProductContent.builder().stock(1).build())
                .overrideFrozen(true)
                .build());

        server.verify(patchRequestedFor(urlEqualTo(PRODUCT_PATH))
                .withRequestBody(matchingJsonPath("$.overrideFrozen", equalTo("true"))));
    }

    @Test
    void omitsTheFieldProjectionEntirelyWhenNoFieldsWereSelected() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        products().search(ProductSearchRequest.all()).count();

        // Layer 1 pre-populates `fields` with all 58 selectable names; sending that on every search
        // would make each response as large as possible and defeat the projection entirely.
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$[?(!@.fields)]")));
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
        server.stubFor(get(urlPathEqualTo(PRODUCT_PATH)).willReturn(okJson(MINIMAL_PRODUCT_BODY)));

        products().get(SKU_1, new java.util.LinkedHashSet<>(
                List.of(ProductField.NAME, ProductField.PRICE)));

        // One comma-joined value, not repeated keys.
        LoggedRequest sent = server.findAll(getRequestedFor(urlPathEqualTo(PRODUCT_PATH))).get(0);
        assertEquals(1, sent.queryParameter("fields").values().size());
        assertTrue(sent.queryParameter("fields").firstValue().contains(","));
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

    @Test
    void widensAProjectionWithTheFieldsAProductCannotBeBuiltWithout() {
        // Erli lets a projection omit any field, including the ones Product needs. Asking for NAME alone
        // would return a body the mapper cannot turn into a Product, so the SDK widens the selection.
        server.stubFor(get(urlPathEqualTo(PRODUCT_PATH)).willReturn(okJson(FULL_PRODUCT_BODY)));

        Product product = products().get(SKU_1, Set.of(ProductField.NAME)).orElseThrow();

        assertEquals("sku-1", product.externalId().value());
        LoggedRequest sent = server.findAll(getRequestedFor(urlPathEqualTo(PRODUCT_PATH))).get(0);
        List<String> requestedFields = List.of(sent.queryParameter("fields").firstValue().split(","));
        assertTrue(requestedFields.containsAll(List.of("name", "externalId", "price", "slug", "created")),
                "the projection must be widened, was " + requestedFields);
        assertFalse(requestedFields.contains("translations"),
                "widening must not pull in the expensive fields the caller did not ask for");
    }

    @Test
    void mapsAProjectedBodyThatCarriesOnlyTheWidenedFields() {
        server.stubFor(get(urlPathEqualTo(PRODUCT_PATH)).willReturn(okJson(MINIMAL_PRODUCT_BODY)));

        Product product = products().get(SKU_1, Set.of(ProductField.NAME, ProductField.PRICE)).orElseThrow();

        assertEquals("sku-1", product.externalId().value());
        assertEquals(new java.math.BigDecimal("100.00"), product.price().amount());
        // Fields outside the projection come back empty rather than blowing up the mapping.
        assertTrue(product.ean().isEmpty());
        assertTrue(product.description().isEmpty());
        assertTrue(product.attributes().isEmpty());
    }

    @Test
    void walksASecondPageUsingTheCursorDerivedFromTheLastRow() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$[?(!@.pagination.after)]"))
                .willReturn(okJson("[" + productBody("sku-a") + "," + productBody("sku-b") + "]")));
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination.after", equalTo("sku-b")))
                .willReturn(okJson("[" + productBody("sku-c") + "]")));

        List<String> walked = products().search(ProductSearchRequest.builder().pageSize(2).build())
                .map(product -> product.externalId().value()).toList();

        assertEquals(List.of("sku-a", "sku-b", "sku-c"), walked);
        // Page two is requested with the last row's sort-field value, because Erli sends no body cursor.
        server.verify(2, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void fetchesOnlyTheFirstPageWhenTheConsumerStopsEarly() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(okJson("[" + productBody("sku-a") + "," + productBody("sku-b") + "]")));

        List<String> firstOnly = products().search(ProductSearchRequest.builder().pageSize(2).build())
                .limit(1).map(product -> product.externalId().value()).toList();

        assertEquals(List.of("sku-a"), firstOnly);
        server.verify(1, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void refusesToPageBeyondTheFirstPageOnASortFieldThatCanRepeat() {
        // Erli's cursor is a strict bound, so products sharing the last row's `updated` would be skipped.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(okJson("[" + productBody("sku-a") + "," + productBody("sku-b") + "]")));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> products().search(ProductSearchRequest.builder()
                        .pageSize(2)
                        .sortBy(ProductSortField.UPDATED, SortOrder.DESC)
                        .build()).toList());

        assertTrue(failure.getMessage().contains("UPDATED"), failure.getMessage());
    }

    @Test
    void deliversAFullFirstPageOnANonUniqueSortWithoutRefusing() {
        // The guard must fire only when a *further* page is asked for. A caller reading exactly one page
        // is never skipping anything, so refusing here would throw away a page already fetched and mapped.
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(okJson("[" + productBody("sku-a") + "," + productBody("sku-b") + "]")));

        List<Product> page = products().search(ProductSearchRequest.builder()
                .pageSize(2)
                .sortBy(ProductSortField.UPDATED, SortOrder.DESC)
                .build()).limit(2).toList();

        assertEquals(2, page.size());
        server.verify(1, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void aShortPageOnANonUniqueSortIsStillAllowed() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[" + productBody("sku-a") + "]")));

        List<Product> page = products().search(ProductSearchRequest.builder()
                .pageSize(2)
                .sortBy(ProductSortField.UPDATED, SortOrder.DESC)
                .build()).toList();

        assertEquals(1, page.size());
    }

    @Test
    void sendsNumericFilterValuesAsJsonNumbersNotStrings() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        products().search(ProductSearchRequest.builder()
                .filter(ProductFilter.greaterThan(ProductFilterField.STOCK, "0"))
                .build()).count();

        // Erli compares a filter value against the column's own type: "0" would simply not match.
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.filter[?(@.value == 0)]")));
    }

    @Test
    void sendsANumericCursorAsAJsonNumberWhenPagingByMarketplaceId() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$[?(!@.pagination.after)]"))
                .willReturn(okJson("[" + productBody("sku-a") + "," + productBody("sku-b") + "]")));
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination[?(@.after == 987654)]"))
                .willReturn(okJson("[]")));

        products().search(ProductSearchRequest.builder()
                .pageSize(2)
                .sortBy(ProductSortField.MARKETPLACE_ID, SortOrder.ASC)
                .build()).toList();

        // A numeric column compared against a quoted cursor would end the walk after one page.
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination[?(@.after == 987654)]")));
    }

    @Test
    void rejectsAFilterValueThatDoesNotMatchTheFieldType() {
        assertThrows(IllegalArgumentException.class,
                () -> products().search(ProductSearchRequest.builder()
                        .filter(ProductFilter.greaterThan(ProductFilterField.STOCK, "plenty"))
                        .build()).count());
        // Boolean.valueOf would have turned this typo into `false` and returned the wrong products.
        assertThrows(IllegalArgumentException.class,
                () -> products().search(ProductSearchRequest.builder()
                        .filter(ProductFilter.equalTo(ProductFilterField.ARCHIVED, "yes"))
                        .build()).count());
    }

    @Test
    void alwaysStatesThePageSizeItUsesToDetectTheLastPage() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        products().search(ProductSearchRequest.all()).count();

        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination.limit",
                        equalTo(Integer.toString(ProductSearchRequest.DEFAULT_PAGE_SIZE)))));
    }

    @Test
    void startDiscountSendsTheMinorUnitPriceAndWindowAndMapsTheResult() {
        server.stubFor(post(urlEqualTo(DISCOUNT_PATH)).willReturn(okJson(DISCOUNT_BODY)));

        Discount discount = products().startDiscount(SKU_1, DiscountRequest.between(
                Money.ofPln("79.00"),
                OffsetDateTime.parse("2026-08-01T00:00:00+02:00"),
                OffsetDateTime.parse("2026-08-08T00:00:00+02:00")));

        server.verify(postRequestedFor(urlEqualTo(DISCOUNT_PATH))
                .withRequestBody(matchingJsonPath("$.newPrice", equalTo("7900")))
                .withRequestBody(matchingJsonPath("$.unfreezeAfterwards", equalTo("true")))
                .withRequestBody(matchingJsonPath("$.startAt"))
                .withRequestBody(matchingJsonPath("$.restoreAt")));
        assertEquals("sku-1", discount.externalId().value());
        assertEquals(100007L, discount.shopId());
        assertEquals(new java.math.BigDecimal("79.00"), discount.newPrice().amount());
        assertTrue(discount.unfreezeAfterwards());
        assertTrue(discount.isActiveAt(OffsetDateTime.parse("2026-08-04T00:00:00+02:00")));
    }

    @Test
    void getDiscountMapsAnExistingPromotion() {
        server.stubFor(get(urlEqualTo(DISCOUNT_PATH)).willReturn(okJson(DISCOUNT_BODY)));

        Discount discount = products().getDiscount(SKU_1).orElseThrow();

        assertEquals(new java.math.BigDecimal("79.00"), discount.newPrice().amount());
        assertFalse(discount.isActiveAt(OffsetDateTime.parse("2026-09-01T00:00:00+02:00")));
    }

    @Test
    void rejectsAMembershipFilterOnAnEqualityOnlyField() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ProductFilter.in(ProductFilterField.STATUS, List.of("active")));
        assertTrue(failure.getMessage().contains("STATUS"), failure.getMessage());
    }

    // --- The mandatory error-path table (TESTING.md) ------------------------------------------------

    @Test
    void mapsAnObservedUnauthorizedBodyToTheAuthException() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH)).willReturn(aResponse()
                .withStatus(401)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody(OBSERVED_401_BODY)));

        assertThrows(ErliAuthException.class, () -> products().update(SKU_1, stockPatch()));
    }

    @Test
    void mapsNotFoundToTheNotFoundExceptionWhenTheCallerDidNotAskAboutExistence() {
        server.stubFor(patch(urlEqualTo(PRODUCT_PATH)).willReturn(aResponse()
                .withStatus(404)
                .withHeader(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .withBody("{\"message\":\"Product not found\"}")));

        assertThrows(ErliNotFoundException.class, () -> products().update(SKU_1, stockPatch()));
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

    /** The smallest meaningful patch: one field set, nothing cleared. */
    private static ProductPatch stockPatch() {
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

}
