package io.github.mgrtomaszzurawski.erli.internal.client.orders;

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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.SellerStatus;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.orders.Order;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderAccess;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderFilter;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderSearchRequest;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderUpdateRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Wire contract for the four {@code /orders} operations, driven through a real {@link ErliClient}
 * against a local WireMock server. Every test verifies the request the SDK <em>produced</em>, not only
 * the response it got back — a stub that answers 200 to anything would otherwise pass while the SDK
 * sent the wrong method, path or body (see {@code TESTING.md}).
 */
class OrderAccessImplTest {

    private static final String SEARCH_PATH = "/orders/_search";
    private static final String ORDER_ID = "221201x12345";
    private static final String ORDER_PATH = "/orders/" + ORDER_ID;
    private static final String STATUS_PATH = ORDER_PATH + "/status";
    private static final String TEST_KEY = "test-key";
    private static final String CURSOR_PAGE_ONE = "1753178730;221201x12345";
    private static final String FULL_ORDER_FIXTURE = "fixtures/orders/order-full.json";
    private static final String MINIMAL_ORDER_FIXTURE = "fixtures/orders/order-minimal.json";
    private static final String MINIMAL_FIXTURE_ORDER_ID = "221202x12346";
    private static final String STATE_DRAINED = "drained";
    private static final String STATE_SECOND_PAGE = "second-page";

    private static final ObjectMapper FIXTURE_MAPPER = new ObjectMapper();

    private WireMockServer server;
    private ErliClient client;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = ErliClient.builder()
                .baseUrl(server.baseUrl())
                .apiKey(ApiKey.of(TEST_KEY))
                .retryPolicy(RetryPolicy.none())
                .build();
    }

    @AfterEach
    void stopServer() {
        client.close();
        server.stop();
    }

    private OrderAccess orders() {
        return client.orders();
    }

    private static String pageOf(String... orderJson) {
        return "[" + String.join(",", orderJson) + "]";
    }

    /**
     * The minimal fixture re-identified, optionally carrying a pagination cursor. Parsed and rebuilt
     * rather than string-spliced, so a change to the fixture surfaces as a parse error here instead of
     * as a malformed request body inside WireMock.
     */
    private static String orderJson(String id, String cursor) {
        try {
            ObjectNode order =
                    (ObjectNode) FIXTURE_MAPPER.readTree(TestFixtures.read(MINIMAL_ORDER_FIXTURE));
            order.put("id", id);
            if (cursor != null) {
                order.put("cursor", cursor);
            }
            return order.toString();
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException(MINIMAL_ORDER_FIXTURE + " is not valid JSON", failure);
        }
    }

    private static String orderJson(String id) {
        return orderJson(id, null);
    }

    // --- search -----------------------------------------------------------------------------------

    @Test
    void searchPostsTheDefaultPaginationBodyAndMapsTheResult() {
        String scenario = "single-page-search";
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson(pageOf(TestFixtures.read(FULL_ORDER_FIXTURE))))
                .willSetStateTo(STATE_DRAINED));
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).inScenario(scenario)
                .whenScenarioStateIs(STATE_DRAINED)
                .willReturn(okJson("[]")));

        List<Order> found = orders().search(OrderSearchRequest.all()).toList();

        assertEquals(1, found.size());
        assertEquals(ORDER_ID, found.get(0).id().value());
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(
                        "{\"pagination\":{\"sortField\":\"updated\",\"order\":\"ASC\",\"limit\":50}}")));
    }

    @Test
    void searchSendsTheRequestedSortDirectionAndPageSize() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        orders().search(OrderSearchRequest.builder()
                .sortBy(OrderSearchRequest.SortField.CREATED)
                .direction(OrderSearchRequest.SortDirection.DESCENDING)
                .pageSize(200)
                .build()).toList();

        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(equalToJson(
                        "{\"pagination\":{\"sortField\":\"created\",\"order\":\"DESC\",\"limit\":200}}")));
    }

    @Test
    void searchOmitsTheCursorOnTheFirstPage() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        orders().search(OrderSearchRequest.all()).toList();

        // An explicit null "after" would be a different request from omitting it; assert it is absent.
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination[?(!@.after)]")));
    }

    @Test
    void searchWalksPagesUsingTheCursorCarriedByTheLastOrderOfThePage() {
        String scenario = "paged-search";
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).inScenario(scenario)
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson(pageOf(orderJson("221201x1", "cursor-a"), orderJson("221201x2", CURSOR_PAGE_ONE))))
                .willSetStateTo(STATE_SECOND_PAGE));
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).inScenario(scenario)
                .whenScenarioStateIs(STATE_SECOND_PAGE)
                .willReturn(okJson("[]")));

        List<Order> found = orders().search(OrderSearchRequest.all()).toList();

        assertEquals(List.of("221201x1", "221201x2"), found.stream().map(order -> order.id().value()).toList());
        // The second request must resume after the LAST order's cursor, not the first one's.
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination.after", equalTo(CURSOR_PAGE_ONE))));
        server.verify(2, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void searchIsLazyAndFetchesOnlyThePagesActuallyConsumed() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(okJson(pageOf(orderJson("221201x1", "cursor-a"), orderJson("221201x2", "cursor-b")))));

        List<Order> firstOnly = orders().search(OrderSearchRequest.all()).limit(1).toList();

        assertEquals(1, firstOnly.size());
        server.verify(1, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    /**
     * The cursor is the only continuation token the endpoint offers, so a page whose last order lacks
     * one ends the walk. Pinned as a deliberate contract rather than left as an accident of
     * {@code orElse(null)} — see {@code OrderAccessImpl.fetchPage}.
     */
    @Test
    void searchStopsWhenTheLastOrderOfAPageCarriesNoCursor() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(okJson(pageOf(orderJson("221201x1", "cursor-a"), orderJson("221201x2")))));

        List<Order> found = orders().search(OrderSearchRequest.all()).toList();

        assertEquals(2, found.size());
        assertTrue(found.get(1).cursor().isEmpty());
        server.verify(1, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void searchStopsWhenAPageComesBackEmpty() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        assertTrue(orders().search(OrderSearchRequest.all()).toList().isEmpty());
        server.verify(1, postRequestedFor(urlEqualTo(SEARCH_PATH)));
    }

    @Test
    void searchResumesFromAnExplicitStartCursor() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        orders().search(OrderSearchRequest.builder()
                .startAfter(Cursor.of(CURSOR_PAGE_ONE))
                .build()).toList();

        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withRequestBody(matchingJsonPath("$.pagination.after", equalTo(CURSOR_PAGE_ONE))));
    }

    @Test
    void searchSerializesANestedFilterTree() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        orders().search(OrderSearchRequest.builder()
                .filter(OrderFilter.and(
                        OrderFilter.paymentCompleted(),
                        OrderFilter.or(
                                OrderFilter.in(OrderFilter.Field.ID, List.of("221201x1", "221201x2")),
                                OrderFilter.not(OrderFilter.userEmail("buyer@example.com")))))
                .build()).toList();

        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH)).withRequestBody(equalToJson("""
                {"pagination":{"sortField":"updated","order":"ASC","limit":50},
                 "filter":{"operator":"and","value":[
                   {"field":"paymentStatus","operator":"=","value":"completed"},
                   {"operator":"or","value":[
                     {"field":"id","operator":"in","value":["221201x1","221201x2"]},
                     {"operator":"not","value":{"field":"userEmail","operator":"=","value":"buyer@example.com"}}
                   ]}
                 ]}}""")));
    }

    // --- byId -------------------------------------------------------------------------------------

    @Test
    void byIdGetsTheTemplatedPathAndMapsTheOrder() {
        server.stubFor(get(urlEqualTo(ORDER_PATH))
                .willReturn(okJson(TestFixtures.read(FULL_ORDER_FIXTURE))));

        Order order = orders().byId(OrderId.of(ORDER_ID));

        assertEquals(ORDER_ID, order.id().value());
        assertEquals("erp-1001", order.externalOrderId().orElseThrow());
        server.verify(getRequestedFor(urlEqualTo(ORDER_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY)));
    }

    // --- update -----------------------------------------------------------------------------------

    @Test
    void updateSendsOnlyTheFieldThatChanged() {
        server.stubFor(patch(urlEqualTo(ORDER_PATH)).willReturn(aResponse().withStatus(202)));

        orders().update(OrderId.of(ORDER_ID), OrderUpdateRequest.ofExternalOrderId("erp-1001"));

        // deliveryTracking must be absent, not null: on a PATCH an explicit null clears the field.
        server.verify(patchRequestedFor(urlEqualTo(ORDER_PATH))
                .withRequestBody(equalToJson("{\"externalOrderId\":\"erp-1001\"}"))
                .withRequestBody(matchingJsonPath("$[?(!@.deliveryTracking)]")));
    }

    @Test
    void updateRejectsARequestThatWouldChangeNothingWithoutCallingTheApi() {
        assertThrows(IllegalArgumentException.class,
                () -> orders().update(OrderId.of(ORDER_ID), OrderUpdateRequest.builder().build()));

        server.verify(0, patchRequestedFor(urlEqualTo(ORDER_PATH)));
    }

    @Test
    void changeStatusPatchesTheStatusSubResource() {
        server.stubFor(patch(urlEqualTo(STATUS_PATH)).willReturn(aResponse().withStatus(204)));

        orders().changeStatus(OrderId.of(ORDER_ID), SellerStatus.READY_TO_PROCESS);

        server.verify(patchRequestedFor(urlEqualTo(STATUS_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withRequestBody(equalToJson("{\"status\":\"readyToProcess\"}")));
    }

    /**
     * A hostile order id must not be able to move the write to another endpoint. Before the id was
     * encoded, {@code "221201x1#"} truncated the path and this call arrived as
     * {@code PATCH /orders/221201x1} — the order-update endpoint — instead of its {@code /status}
     * sub-resource.
     */
    @Test
    void changeStatusCannotBeRedirectedByAHostileOrderId() {
        String hostileId = ORDER_ID + "#";
        String encodedStatusPath = "/orders/" + ORDER_ID + "%23/status";
        server.stubFor(patch(urlEqualTo(encodedStatusPath)).willReturn(aResponse().withStatus(204)));

        orders().changeStatus(OrderId.of(hostileId), SellerStatus.SENT);

        server.verify(patchRequestedFor(urlEqualTo(encodedStatusPath)));
        server.verify(0, patchRequestedFor(urlEqualTo(ORDER_PATH)));
    }

    @Test
    void byIdCannotHaveAQueryStringInjectedThroughTheOrderId() {
        String encodedPath = "/orders/" + ORDER_ID + "%3Flimit%3D999";
        server.stubFor(get(urlEqualTo(encodedPath))
                .willReturn(okJson(TestFixtures.read(FULL_ORDER_FIXTURE))));

        orders().byId(OrderId.of(ORDER_ID + "?limit=999"));

        server.verify(getRequestedFor(urlEqualTo(encodedPath)));
    }

    @Test
    void aClosedClientRejectsFurtherUse() {
        client.close();

        assertThrows(IllegalStateException.class, () -> client.orders());
    }

    // --- the mandatory error-path table -----------------------------------------------------------

    @ParameterizedTest(name = "HTTP {0} maps to {1}")
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
    void mapsEveryErrorStatusToItsRemediationException(int status, Class<? extends ErliException> expected) {
        server.stubFor(get(urlEqualTo(ORDER_PATH)).willReturn(aResponse().withStatus(status)));

        ErliException thrown = assertThrows(ErliException.class, () -> orders().byId(OrderId.of(ORDER_ID)));

        assertInstanceOf(expected, thrown);
    }

    @Test
    void mapsTheObservedNotFoundBodyPreservingTraceAndPolishMessage() {
        // Shaped after the live 401 recorded in KNOWN-SERVER-BEHAVIORS: the payload is richer than the
        // spec (httpCode, failureType) and traceId may be missing entirely.
        String body = """
                {"code":1400,"failureType":"notFound","message":"Order not found",
                 "polishMessage":"Nie znaleziono zamowienia","httpCode":404,"spanId":"span-7"}""";
        server.stubFor(get(urlEqualTo(ORDER_PATH))
                .willReturn(aResponse().withStatus(404).withBody(body)));

        ErliNotFoundException thrown = assertThrows(ErliNotFoundException.class,
                () -> orders().byId(OrderId.of(ORDER_ID)));

        assertEquals("notFound", thrown.details().failureType());
        assertEquals("span-7", thrown.details().spanId());
        assertEquals("Nie znaleziono zamowienia", thrown.details().polishMessage());
    }

    @Test
    void mapsAValidationFailureOnAWriteToTheValidationException() {
        server.stubFor(patch(urlEqualTo(ORDER_PATH))
                .willReturn(aResponse().withStatus(400)
                        .withBody("{\"code\":1200,\"message\":\"Order is cancelled\"}")));

        ErliValidationException thrown = assertThrows(ErliValidationException.class, () -> orders()
                .update(OrderId.of(ORDER_ID), OrderUpdateRequest.ofExternalOrderId("erp-1")));

        assertTrue(thrown.getMessage().contains("Order is cancelled"), thrown.getMessage());
    }

    @Test
    void mapsANonJsonErrorBodyWithoutLosingIt() {
        server.stubFor(patch(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse().withStatus(502).withBody("<html>Bad Gateway</html>")));

        ErliServerException thrown = assertThrows(ErliServerException.class,
                () -> orders().changeStatus(OrderId.of(ORDER_ID), SellerStatus.SENT));

        assertTrue(thrown.details().rawBody().contains("Bad Gateway"), thrown.details().rawBody());
    }

    @Test
    void authFailureOnSearchSurfacesAsAnAuthException() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH))
                .willReturn(aResponse().withStatus(401)
                        .withBody("{\"failureType\":\"security\",\"message\":\"Invalid API key\"}")));

        ErliAuthException thrown = assertThrows(ErliAuthException.class,
                () -> orders().search(OrderSearchRequest.all()).toList());

        assertEquals("security", thrown.details().failureType());
    }
}
