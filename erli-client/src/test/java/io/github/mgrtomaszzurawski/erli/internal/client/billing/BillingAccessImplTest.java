package io.github.mgrtomaszzurawski.erli.internal.client.billing;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntry;
import io.github.mgrtomaszzurawski.erli.domain.billing.BillingEntryFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify-on-write coverage for the billing area, with particular attention to the derived cursor:
 * the response is a bare array with no pagination envelope, so the SDK builds the next
 * {@code pagination.after} from the last entry's id.
 */
class BillingAccessImplTest {

    private static final String ENTRIES_PATH = "/billing/company/entries";
    private static final String REBATES_PATH = "/billing/company/rebates";
    private static final String API_KEY_VALUE = "100007:test-secret";

    /** Built from the BillingEntriesResponse item schema; the sandbox ledger is empty. */
    private static final String TWO_ENTRIES_BODY = """
            [{"id":52,"occurredAt":"2026-07-24T14:45:47.540+02:00","type":"COMMISSION",
              "description":"Prowizja od sprzedaży","amount":-1168,"balanceAfter":48832,
              "orderId":"202607x1234","shopId":100007,"productId":9001},
             {"id":51,"occurredAt":"2026-07-23T09:00:00.000+02:00","type":"REBATE",
              "description":"Rabat","amount":250,"balanceAfter":50000,
              "rebateOrigin":[{"amount":150,"rebateReason":"PROMO"},{"amount":100,"rebateReason":"LOYALTY"}]}]""";

    private static final String ONE_ENTRY_BODY = """
            [{"id":50,"occurredAt":"2026-07-22T09:00:00.000+02:00","type":"COMMISSION",
              "description":"Prowizja","amount":-100,"balanceAfter":49900}]""";

    private static final String EMPTY_BODY = "[]";

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
    void asksForTheOnlySupportedSortAndMapsEveryField() {
        server.stubFor(post(urlEqualTo(ENTRIES_PATH))
                .willReturn(aResponse().withStatus(200).withBody(TWO_ENTRIES_BODY)));

        List<BillingEntry> entries = client.billing().entries(BillingEntryFilter.all()).toList();

        assertEquals(2, entries.size());
        BillingEntry commission = entries.get(0);
        assertEquals(52L, commission.id());
        assertEquals(OffsetDateTime.parse("2026-07-24T14:45:47.540+02:00"), commission.occurredAt());
        assertEquals("COMMISSION", commission.type());
        assertEquals("Prowizja od sprzedaży", commission.description());
        assertEquals(Money.ofPln("-11.68"), commission.amount());
        assertEquals(Money.ofPln("488.32"), commission.balanceAfter());
        assertEquals(OrderId.of("202607x1234"), commission.orderId().orElseThrow());
        assertEquals(100007L, commission.shopId().orElseThrow());
        assertEquals(9001L, commission.productId().orElseThrow());
        assertTrue(commission.rebateOrigin().isEmpty());

        BillingEntry rebate = entries.get(1);
        assertEquals(2, rebate.rebateOrigin().size());
        assertEquals(Money.ofPln("1.50"), rebate.rebateOrigin().get(0).amount());
        assertEquals("PROMO", rebate.rebateOrigin().get(0).rebateReason());
        assertTrue(rebate.orderId().isEmpty());

        // The API supports only id/DESC, so the request states it rather than relying on defaults.
        server.verify(postRequestedFor(urlEqualTo(ENTRIES_PATH))
                .withHeader("Authorization", equalTo("Bearer " + API_KEY_VALUE))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        {"pagination":{"sortField":"id","order":"DESC","limit":100},"simpleFilter":{}}""")));
    }

    @Test
    void writesEveryFilterCriterionAndNeverAnExplicitNull() {
        server.stubFor(post(urlEqualTo(ENTRIES_PATH))
                .willReturn(aResponse().withStatus(200).withBody(EMPTY_BODY)));

        client.billing().entries(BillingEntryFilter.builder()
                .type("COMMISSION")
                .fromOccurredAt(OffsetDateTime.parse("2026-07-01T00:00:00Z"))
                .toOccurredAt(OffsetDateTime.parse("2026-07-31T00:00:00Z"))
                .orderId(OrderId.of("202607x1234"))
                .shopId(100007L)
                .productId(9001L)
                .pageSize(250)
                .build()).toList();

        server.verify(postRequestedFor(urlEqualTo(ENTRIES_PATH))
                .withRequestBody(matchingJsonPath("$.simpleFilter.type", equalTo("COMMISSION")))
                .withRequestBody(matchingJsonPath("$.simpleFilter.orderId", equalTo("202607x1234")))
                .withRequestBody(matchingJsonPath("$.simpleFilter.shopId", equalTo("100007")))
                .withRequestBody(matchingJsonPath("$.simpleFilter.productId", equalTo("9001")))
                .withRequestBody(matchingJsonPath("$.pagination.limit", equalTo("250"))));

        String body = server.getAllServeEvents().get(0).getRequest().getBodyAsString();
        assertTrue(body.contains("fromOccurredAt"), body);
        assertTrue(!body.contains("null"), "the API rejects explicit nulls: " + body);
        assertTrue(!body.contains("\"after\""), "no cursor on the first page: " + body);
    }

    @Test
    void derivesTheNextCursorFromTheLastEntryIdAndStopsOnAShortPage() {
        // Page size 2: a full page means "ask again after id 51", a short page ends the walk.
        server.stubFor(post(urlEqualTo(ENTRIES_PATH))
                .withRequestBody(matchingJsonPath("$.pagination[?(!@.after)]"))
                .willReturn(aResponse().withStatus(200).withBody(TWO_ENTRIES_BODY)));
        server.stubFor(post(urlEqualTo(ENTRIES_PATH))
                .withRequestBody(matchingJsonPath("$.pagination.after", equalTo("51")))
                .willReturn(aResponse().withStatus(200).withBody(ONE_ENTRY_BODY)));

        List<Long> ids = client.billing()
                .entries(BillingEntryFilter.builder().pageSize(2).build())
                .map(BillingEntry::id)
                .toList();

        assertEquals(List.of(52L, 51L, 50L), ids);
        server.verify(2, postRequestedFor(urlEqualTo(ENTRIES_PATH)));
    }

    @Test
    void fetchesLazilySoATakeOfOneCostsASingleRequest() {
        server.stubFor(post(urlEqualTo(ENTRIES_PATH))
                .willReturn(aResponse().withStatus(200).withBody(TWO_ENTRIES_BODY)));

        List<Long> ids = client.billing()
                .entries(BillingEntryFilter.builder().pageSize(2).build())
                .limit(1)
                .map(BillingEntry::id)
                .toList();

        assertEquals(List.of(52L), ids);
        server.verify(1, postRequestedFor(urlEqualTo(ENTRIES_PATH)));
    }

    @Test
    void rebatesUseTheirOwnEndpointWithTheSameShape() {
        server.stubFor(post(urlEqualTo(REBATES_PATH))
                .willReturn(aResponse().withStatus(200).withBody(TWO_ENTRIES_BODY)));

        String reasons = client.billing().rebates(BillingEntryFilter.all())
                .flatMap(entry -> entry.rebateOrigin().stream())
                .map(origin -> origin.rebateReason())
                .collect(Collectors.joining(","));

        assertEquals("PROMO,LOYALTY", reasons);
        server.verify(1, postRequestedFor(urlEqualTo(REBATES_PATH)));
        server.verify(0, postRequestedFor(urlEqualTo(ENTRIES_PATH)));
    }

    @Test
    void rejectsAPageSizeAboveTheApiCap() {
        assertThrows(IllegalArgumentException.class,
                () -> BillingEntryFilter.builder().pageSize(BillingEntryFilter.MAX_PAGE_SIZE + 1).build());
        assertThrows(IllegalArgumentException.class,
                () -> BillingEntryFilter.builder().pageSize(0).build());
    }
}
