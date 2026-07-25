package io.github.mgrtomaszzurawski.erli.internal.client.campaigns;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignCostSummary;
import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignDailyCost;
import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verify-on-write coverage for the campaigns area, including the required query parameters. */
class CampaignsAccessImplTest {

    private static final String SUMMARY_PATH = "/campaigns/campaigns-summary";
    private static final String API_KEY_VALUE = "100007:test-secret";
    private static final String USER_AGENT_PATTERN = "erli-java-sdk/.*";

    private static final LocalDate RANGE_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate RANGE_END = LocalDate.of(2026, 7, 20);

    /** Shape observed live on the sandbox, 2026-07-25 (empty shop returns an empty data array). */
    private static final String EMPTY_SUMMARY_BODY = "{\"shopId\":100007,\"data\":[]}";

    /** Same shape with rows, built from the ShopCampaignsCostSummaryResponse schema. */
    private static final String POPULATED_SUMMARY_BODY = """
            {"shopId":100007,"data":[
              {"campaignId":"12-company-34-shop-summer","date":"2026-06-01T00:00:00.000+02:00","netShopCost":12345},
              {"netShopCost":500}
            ]}""";

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
    void sendsBothDatesAsQueryParametersInYearMonthDayForm() {
        server.stubFor(get(urlPathEqualTo(SUMMARY_PATH))
                .willReturn(aResponse().withStatus(200).withBody(EMPTY_SUMMARY_BODY)));

        CampaignCostSummary summary = client.campaigns().costSummary(RANGE_START, RANGE_END);

        assertEquals(100007L, summary.shopId());
        assertTrue(summary.dailyCosts().isEmpty());
        server.verify(getRequestedFor(urlEqualTo(SUMMARY_PATH + "?startDate=2026-06-01&endDate=2026-07-20"))
                .withHeader("Authorization", equalTo("Bearer " + API_KEY_VALUE))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("User-Agent", matching(USER_AGENT_PATTERN)));
    }

    @Test
    void mapsGroszeCostsAndOptionalColumns() {
        server.stubFor(get(urlPathEqualTo(SUMMARY_PATH))
                .willReturn(aResponse().withStatus(200).withBody(POPULATED_SUMMARY_BODY)));

        CampaignCostSummary summary = client.campaigns().costSummary(RANGE_START, RANGE_END);

        assertEquals(2, summary.dailyCosts().size());
        CampaignDailyCost attributed = summary.dailyCosts().get(0);
        assertEquals(Money.ofPln("123.45"), attributed.netShopCost());
        assertEquals(CampaignId.of("12-company-34-shop-summer"), attributed.campaignId().orElseThrow());
        assertEquals(OffsetDateTime.parse("2026-06-01T00:00:00.000+02:00"), attributed.date().orElseThrow());

        CampaignDailyCost unattributed = summary.dailyCosts().get(1);
        assertEquals(Money.ofPln("5.00"), unattributed.netShopCost());
        assertTrue(unattributed.campaignId().isEmpty());
        assertTrue(unattributed.date().isEmpty());
    }

    @Test
    void sumsTheRangeIntoOneTotal() {
        server.stubFor(get(urlPathEqualTo(SUMMARY_PATH))
                .willReturn(aResponse().withStatus(200).withBody(POPULATED_SUMMARY_BODY)));

        CampaignCostSummary summary = client.campaigns().costSummary(RANGE_START, RANGE_END);

        assertEquals(Money.ofPln("128.45"), summary.totalNetCost());
    }

    @Test
    void mapsTheServersFutureEndDateRejectionToValidationException() {
        // Real observed body, 2026-07-25: endDate past the marketplace's current time.
        String body = """
                {"failureType":"validation","message":"\\"endDate\\" must be less than or equal to \
                \\"2026-07-24T14:45:47.540+02:00\\"","errorCode":"VAL-QUERY",\
                "params":[{"path":["endDate"],"code":"date.max"}],"httpCode":400,\
                "polishMessage":"Problem z walidacją, sprawdź pola",\
                "validationDetails":[{"message":"Zbyt późna data","path":["endDate"],"type":"date.max"}],\
                "payload":{"isJoi":true},"spanId":"699t9AzTJbgZ2","traceId":"699t9AzTJbgZ2"}""";
        server.stubFor(get(urlPathEqualTo(SUMMARY_PATH))
                .willReturn(aResponse().withStatus(400).withBody(body)));

        ErliValidationException failure = assertThrows(ErliValidationException.class,
                () -> client.campaigns().costSummary(RANGE_START, LocalDate.of(2030, 1, 1)));

        assertEquals("699t9AzTJbgZ2", failure.details().traceId());
        assertEquals("validation", failure.details().failureType());
    }

    @Test
    void rejectsAnInvertedRangeBeforeTouchingTheWire() {
        assertThrows(IllegalArgumentException.class,
                () -> client.campaigns().costSummary(RANGE_END, RANGE_START));

        server.verify(0, getRequestedFor(urlPathEqualTo(SUMMARY_PATH)));
    }

    @Test
    void requiresBothDates() {
        assertThrows(NullPointerException.class, () -> client.campaigns().costSummary(null, RANGE_END));
        assertThrows(NullPointerException.class, () -> client.campaigns().costSummary(RANGE_START, null));

        server.verify(0, getRequestedFor(urlPathEqualTo(SUMMARY_PATH)));
    }
}
