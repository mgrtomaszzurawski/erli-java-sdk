package io.github.mgrtomaszzurawski.erli.internal.client.shipping;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.model.OrderId;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.shipping.CarrierPoint;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelDraft;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelResult;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ExternalParcelUpdate;
import io.github.mgrtomaszzurawski.erli.domain.shipping.Parcel;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelDimensions;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelDraft;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelFilter;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelSearchField;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ParcelStatus;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PickupType;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PostingPoint;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PostingPointQuery;
import io.github.mgrtomaszzurawski.erli.domain.shipping.PostingPointType;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingCountry;
import io.github.mgrtomaszzurawski.erli.domain.shipping.ShippingParty;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verify-on-write cover for the shipping bucket's nine write and query operations. */
class ShippingWriteOperationsTest {

    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String PARCELS_PATH = "/shipping/parcels/";
    private static final String PARCELS_SEARCH_PATH = "/shipping/parcels/_search";
    private static final String PARCEL_BY_ID_PATH = "/shipping/parcels/55123";
    private static final String EXTERNAL_PATH = "/shipping/external";
    private static final String EXTERNAL_BY_ID_PATH = "/shipping/external/77";
    private static final String PICKUP_PROTOCOLS_PATH = "/shipping/pickupProtocols";
    private static final String POSTING_POINTS_PATH = "/shipping/postingPoints";
    private static final int HTTP_NO_CONTENT = 204;

    private static final String EXTERNAL_PARCEL_JSON = """
            { "id": 77, "orderId": "100007x1234", "type": "external",
              "shipping": { "vendor": "dpd" }, "status": "sent", "trackingNumber": "TRK-1",
              "createdAt": "2026-07-20T08:14:00Z", "updatedAt": "2026-07-21T09:30:00Z" }
            """;
    private static final String EXTERNAL_BATCH_JSON = """
            [ { "id": 77, "orderId": "100007x1234", "type": "external",
                "shipping": { "vendor": "dpd" }, "status": "sent",
                "createdAt": "2026-07-20T08:14:00Z", "updatedAt": "2026-07-21T09:30:00Z" },
              { "orderId": "100007x9999", "vendor": "dhl",
                "error": [ { "errorCode": 1110, "errorMessage": "Nie mozna dodac paczki do anulowanego zamowienia" } ] } ]
            """;
    private static final String POSTING_POINTS_JSON = """
            [ { "id": 4471, "name": "Magazyn", "type": "point", "isDefault": true,
                "companyName": "Test Shop", "phone": "500100200", "email": "shop@example.test",
                "street": "Przemyslowa", "buildingNumber": "12", "zip": "61-001", "city": "Poznan",
                "pointCode": "POZ01A",
                "pointAddress": { "street": "Glogowska", "buildingNumber": "1", "zip": "60-001", "city": "Poznan" },
                "location": { "latitude": 52.4064, "longitude": 16.9252 } } ]
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

    private ShippingAccessImpl shippingAccess() {
        JsonCodec codec = new JsonCodec();
        HttpRuntime runtime = new HttpRuntime(HttpClient.newHttpClient(), server.baseUrl(), ApiKey.of(TEST_KEY),
                RetryPolicy.builder().maxAttempts(2).baseDelay(Duration.ofMillis(1))
                        .maxDelay(Duration.ofMillis(2)).randomGenerator(new Random(0)).build(),
                USER_AGENT, Duration.ofSeconds(5), codec, new ErrorMapper(codec));
        return new ShippingAccessImpl(runtime, codec);
    }

    private static ParcelDraft sampleDraft() {
        ShippingParty receiver = new ShippingParty(
                Optional.of("Jan"), Optional.of("Nowak"), Optional.empty(),
                Optional.of("Kwiatowa"), Optional.of("7"), Optional.empty(),
                Optional.of("Warszawa"), Optional.of("00-950"), Optional.of(ShippingCountry.PL),
                Optional.of("600300400"), Optional.of("buyer@example.test"),
                Optional.of(PickupType.COURIER), Optional.empty());
        return ParcelDraft.builder(OrderId.of("100007x1234"), ShippingMethodId.of("erliDHL5kg"),
                        new ParcelDimensions(new BigDecimal("205"), new BigDecimal("100"),
                                new BigDecimal("300"), 1500),
                        receiver)
                .additionalInformation("Leave at reception")
                .build();
    }

    @Test
    void postsTheCreateParcelsBodyWithDimensionsAndReceiver() {
        server.stubFor(post(urlEqualTo(PARCELS_PATH))
                .willReturn(okJson("[" + ParcelFixtures.MINIMAL_PARCEL_JSON + "]")));

        List<Parcel> created = shippingAccess().createParcels(List.of(sampleDraft()));

        assertEquals(1, created.size());
        server.verify(postRequestedFor(urlEqualTo(PARCELS_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        [ { "orderId": "100007x1234",
                            "dimensions": { "width": 205, "height": 100, "length": 300, "weight": 1500 },
                            "shipping": { "typeId": "erliDHL5kg",
                              "additionalInformation": "Leave at reception",
                              "receiver": { "firstName": "Jan", "lastName": "Nowak", "street": "Kwiatowa",
                                "buildingNumber": "7", "city": "Warszawa", "zip": "00-950", "country": "pl",
                                "phoneNumber": "600300400", "email": "buyer@example.test",
                                "pickupType": "courier" } } } ]
                        """, true, true)));
    }

    @Test
    void postsASearchFilterInTheShapeTheApiExpects() {
        server.stubFor(post(urlEqualTo(PARCELS_SEARCH_PATH)).willReturn(okJson("[]")));

        shippingAccess().searchParcels(ParcelFilter.isEqualTo(ParcelSearchField.ORDER_ID, "100007x1234"));

        server.verify(postRequestedFor(urlEqualTo(PARCELS_SEARCH_PATH))
                .withRequestBody(equalToJson("""
                        { "filter": { "field": "orderId", "operator": "=", "value": "100007x1234" } }
                        """, true, true)));
    }

    @Test
    void sendsAMembershipFilterAsAListValue() {
        server.stubFor(post(urlEqualTo(PARCELS_SEARCH_PATH)).willReturn(okJson("[]")));

        shippingAccess().searchParcels(ParcelFilter.isAnyOf(ParcelSearchField.ID, List.of("1", "2")));

        server.verify(postRequestedFor(urlEqualTo(PARCELS_SEARCH_PATH))
                .withRequestBody(equalToJson("""
                        { "filter": { "field": "id", "operator": "in", "value": [ "1", "2" ] } }
                        """, true, true)));
    }

    @Test
    void cancelsAParcelWithDeleteOnTheTemplatedPath() {
        server.stubFor(delete(urlEqualTo(PARCEL_BY_ID_PATH))
                .willReturn(okJson(ParcelFixtures.MINIMAL_PARCEL_JSON)));

        Parcel cancelled = shippingAccess().cancelParcel(ParcelId.of("55123"));

        assertEquals(ParcelStatus.PREPARING, cancelled.status());
        server.verify(deleteRequestedFor(urlEqualTo(PARCEL_BY_ID_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY)));
    }

    @Test
    void splitsABatchExternalRegistrationIntoCreatedAndRejectedResults() {
        server.stubFor(post(urlEqualTo(EXTERNAL_PATH)).willReturn(okJson(EXTERNAL_BATCH_JSON)));

        List<ExternalParcelResult> results = shippingAccess().registerExternalParcels(List.of(
                ExternalParcelDraft.builder(OrderId.of("100007x1234"), DeliveryVendor.DPD)
                        .trackingNumber("TRK-1").build()));

        assertEquals(2, results.size());
        ExternalParcelResult.Created created = assertInstanceOf(ExternalParcelResult.Created.class, results.get(0));
        assertEquals(DeliveryVendor.DPD, created.parcel().vendor());
        ExternalParcelResult.Rejected rejected =
                assertInstanceOf(ExternalParcelResult.Rejected.class, results.get(1));
        assertEquals(OrderId.of("100007x9999"), rejected.orderId());
        assertEquals(1110, rejected.errors().get(0).errorCode());
        server.verify(postRequestedFor(urlEqualTo(EXTERNAL_PATH))
                .withRequestBody(equalToJson("""
                        [ { "orderId": "100007x1234", "vendor": "dpd", "trackingNumber": "TRK-1" } ]
                        """, true, true)));
    }

    @Test
    void readsAndPatchesAnExternalParcel() {
        server.stubFor(get(urlEqualTo(EXTERNAL_BY_ID_PATH)).willReturn(okJson(EXTERNAL_PARCEL_JSON)));
        server.stubFor(patch(urlEqualTo(EXTERNAL_BY_ID_PATH)).willReturn(okJson(EXTERNAL_PARCEL_JSON)));

        ExternalParcel fetched = shippingAccess().externalParcel(ParcelId.of("77"));
        shippingAccess().updateExternalParcel(ParcelId.of("77"),
                ExternalParcelUpdate.builder(DeliveryVendor.DPD).trackingNumber("TRK-2").build());

        assertEquals(OrderId.of("100007x1234"), fetched.orderId());
        assertEquals(DeliveryVendor.DPD, fetched.vendor());
        assertEquals(ParcelStatus.SENT, fetched.status());
        server.verify(getRequestedFor(urlEqualTo(EXTERNAL_BY_ID_PATH)));
        server.verify(patchRequestedFor(urlEqualTo(EXTERNAL_BY_ID_PATH))
                .withRequestBody(equalToJson("{ \"vendor\": \"dpd\", \"trackingNumber\": \"TRK-2\" }", true, true)));
    }

    @Test
    void deletesAnExternalParcelAndToleratesTheEmptyNoContentBody() {
        server.stubFor(delete(urlEqualTo(EXTERNAL_BY_ID_PATH))
                .willReturn(aResponse().withStatus(HTTP_NO_CONTENT)));

        shippingAccess().deleteExternalParcel(ParcelId.of("77"));

        server.verify(deleteRequestedFor(urlEqualTo(EXTERNAL_BY_ID_PATH)));
    }

    @Test
    void refusesAStatusTheExternalEndpointCannotSet() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> shippingAccess().updateExternalParcel(ParcelId.of("77"),
                        ExternalParcelUpdate.builder(DeliveryVendor.DPD).status(ParcelStatus.CLAIMED).build()));

        assertTrue(failure.getMessage().contains("CLAIMED"), failure.getMessage());
        server.verify(0, patchRequestedFor(urlEqualTo(EXTERNAL_BY_ID_PATH)));
    }

    @Test
    void sendsPickupProtocolParcelIdsAsOneCommaSeparatedParameter() {
        server.stubFor(get(urlPathEqualTo(PICKUP_PROTOCOLS_PATH))
                .willReturn(okJson("{ \"url\": \"https://erli.pl/protocol/1.pdf\" }")));

        String url = shippingAccess()
                .pickupProtocols(List.of(ParcelId.of("1"), ParcelId.of("2"), ParcelId.of("3"))).url();

        assertEquals("https://erli.pl/protocol/1.pdf", url);
        server.verify(getRequestedFor(urlPathEqualTo(PICKUP_PROTOCOLS_PATH))
                .withQueryParam("parcelIds", equalTo("1,2,3")));
    }

    @Test
    void mapsAPostingPointWithItsCarrierPointAndCoordinates() {
        server.stubFor(get(urlPathEqualTo(POSTING_POINTS_PATH)).willReturn(okJson(POSTING_POINTS_JSON)));

        List<PostingPoint> points =
                shippingAccess().postingPoints(PostingPointQuery.builder().onlyDefault().build());

        PostingPoint postingPoint = points.get(0);
        assertEquals(4471L, postingPoint.id());
        assertEquals("Magazyn", postingPoint.name());
        assertEquals(PostingPointType.POINT, postingPoint.type());
        assertTrue(postingPoint.isDefault());
        CarrierPoint carrierPoint = postingPoint.point().orElseThrow();
        assertEquals(Optional.of("POZ01A"), carrierPoint.pointCode());
        assertEquals(Optional.of("Glogowska"), carrierPoint.pointAddress().orElseThrow().street());
        assertEquals(new BigDecimal("52.4064"), carrierPoint.location().orElseThrow().latitude());
        server.verify(getRequestedFor(urlPathEqualTo(POSTING_POINTS_PATH))
                .withQueryParam("isDefault", equalTo("true")));
    }

    @Test
    void refusesAnEmptyParcelBatchWithoutSendingAnything() {
        assertThrows(IllegalArgumentException.class, () -> shippingAccess().createParcels(List.of()));

        server.verify(0, postRequestedFor(urlEqualTo(PARCELS_PATH)));
    }

    @Test
    void refusesAnEmptyExternalBatchWithoutSendingAnything() {
        assertThrows(IllegalArgumentException.class,
                () -> shippingAccess().registerExternalParcels(List.of()));

        server.verify(0, postRequestedFor(urlEqualTo(EXTERNAL_PATH)));
    }

    @Test
    void refusesAnEmptyPickupProtocolRequestWithoutSendingAnything() {
        assertThrows(IllegalArgumentException.class, () -> shippingAccess().pickupProtocols(List.of()));

        server.verify(0, getRequestedFor(urlPathEqualTo(PICKUP_PROTOCOLS_PATH)));
    }

    @Test
    void refusesARelativePathSegmentOnTheDestructiveDeleteVerbs() {
        assertThrows(IllegalArgumentException.class,
                () -> shippingAccess().cancelParcel(ParcelId.of("..")));
        assertThrows(IllegalArgumentException.class,
                () -> shippingAccess().deleteExternalParcel(ParcelId.of(".")));

        // Asserting on every request the server saw, not on a guessed path: the client transmits dot
        // segments un-normalized, so a path-specific check would match nothing whether or not the guard
        // exists. Collapsing "." to the collection endpoint is what turns one cancel into a bulk delete.
        assertTrue(server.findAll(RequestPatternBuilder.allRequests()).isEmpty(),
                "no request may leave the client for a relative-segment id");
    }

    @Test
    void treatsAnExplicitNullErrorAsACreatedParcelRatherThanARefusal() {
        server.stubFor(post(urlEqualTo(EXTERNAL_PATH)).willReturn(okJson("""
                [ { "id": 77, "orderId": "100007x1234", "type": "external", "error": null,
                    "shipping": { "vendor": "dpd" }, "status": "sent",
                    "createdAt": "2026-07-20T08:14:00Z", "updatedAt": "2026-07-21T09:30:00Z" } ]
                """)));

        List<ExternalParcelResult> results = shippingAccess().registerExternalParcels(List.of(
                ExternalParcelDraft.builder(OrderId.of("100007x1234"), DeliveryVendor.DPD).build()));

        assertInstanceOf(ExternalParcelResult.Created.class, results.get(0));
    }

    @Test
    void degradesAnUnknownStatusOnBothExternalReadPaths() {
        server.stubFor(get(urlEqualTo(EXTERNAL_BY_ID_PATH)).willReturn(okJson("""
                { "id": 77, "orderId": "100007x1234", "type": "external",
                  "shipping": { "vendor": "dpd" }, "status": "handedToDrone",
                  "statusHistory": [ { "status": "alsoUnknown" } ],
                  "createdAt": "2026-07-20T08:14:00Z", "updatedAt": "2026-07-21T09:30:00Z" }
                """)));
        server.stubFor(post(urlEqualTo(EXTERNAL_PATH)).willReturn(okJson("""
                [ { "id": 77, "orderId": "100007x1234", "type": "external",
                    "shipping": { "vendor": "dpd" }, "status": "handedToDrone",
                    "createdAt": "2026-07-20T08:14:00Z", "updatedAt": "2026-07-21T09:30:00Z" } ]
                """)));

        ExternalParcel fetched = shippingAccess().externalParcel(ParcelId.of("77"));
        List<ExternalParcelResult> batch = shippingAccess().registerExternalParcels(List.of(
                ExternalParcelDraft.builder(OrderId.of("100007x1234"), DeliveryVendor.DPD).build()));

        // Each of the three generated status enums has its own overload; one regression would ship
        // green if only the plain read were pinned.
        assertEquals(ParcelStatus.UNRECOGNIZED, fetched.status());
        assertEquals(ParcelStatus.UNRECOGNIZED, fetched.statusHistory().get(0).status());
        assertEquals(ParcelStatus.UNRECOGNIZED,
                assertInstanceOf(ExternalParcelResult.Created.class, batch.get(0)).parcel().status());
        // The rest of the payload must survive — that is the point of degrading rather than failing.
        assertEquals(OrderId.of("100007x1234"), fetched.orderId());
    }

    @Test
    void mapsTheStatusHistoryOfAnExternalParcel() {
        server.stubFor(get(urlEqualTo(EXTERNAL_BY_ID_PATH)).willReturn(okJson("""
                { "id": 77, "orderId": "100007x1234", "type": "external",
                  "shipping": { "vendor": "dpd" }, "status": "sent",
                  "statusHistory": [ { "status": "preparing", "changed": "2026-07-20T08:15:00Z" },
                                     { "status": "sent" } ],
                  "createdAt": "2026-07-20T08:14:00Z", "updatedAt": "2026-07-21T09:30:00Z" }
                """)));

        ExternalParcel parcel = shippingAccess().externalParcel(ParcelId.of("77"));

        assertEquals(2, parcel.statusHistory().size());
        assertEquals(ParcelStatus.PREPARING, parcel.statusHistory().get(0).status());
        assertTrue(parcel.statusHistory().get(0).changed().isPresent());
        assertTrue(parcel.statusHistory().get(1).changed().isEmpty());
    }

    @Test
    void keepsCourierInstructionsOutOfADraftsToString() {
        String rendered = sampleDraft().toString();

        assertFalse(rendered.contains("Leave at reception"), rendered);
        assertTrue(rendered.contains("additionalInformation=***"), rendered);
        // The receiver redacts itself; this pins that the draft does not undo that.
        assertFalse(rendered.contains("buyer@example.test"), rendered);
        assertFalse(rendered.contains("Kwiatowa"), rendered);
    }
}
