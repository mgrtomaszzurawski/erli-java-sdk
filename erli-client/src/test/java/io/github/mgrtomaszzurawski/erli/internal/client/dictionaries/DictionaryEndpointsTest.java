package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.AttachmentKind;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.core.model.Market;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentRemoval;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.AttachmentUpdate;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Category;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.CountryCode;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethodQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewAttachment;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.PriceListName;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ProductAttachmentResult;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyUpdate;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethodQuery;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingOperator;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-level tests for the dictionary endpoints beyond the starter slice: every test asserts the
 * request the SDK produced (verb, path, query string, body), not only the response it decoded.
 */
class DictionaryEndpointsTest {

    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String MEDIA_TYPE_JSON = "application/json";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CATEGORY_SEARCH_PATH = "/dictionaries/category/_search";
    private static final String RESPONSIBLE_PERSONS_PATH = "/dictionaries/responsiblePersons";
    private static final String DELIVERY_METHODS_PATH = "/dictionaries/deliveryMethods";
    private static final String RESPONSIBLE_PRODUCERS_PATH = "/dictionaries/responsibleProducers";
    private static final String ATTACHMENTS_PATH = "/dictionaries/attachments";
    private static final String ATTACH_PATH = "/dictionaries/attachment/attach";
    private static final String DETACH_PATH = "/dictionaries/attachment/detach";
    private static final int CATEGORY_PAGE_SIZE = 200;

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

    private DictionariesAccessImpl dictionaries() {
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
        return new DictionariesAccessImpl(runtime, codec);
    }

    @Test
    void sendsDeliveryMethodFiltersAsAQueryString() {
        String expectedUrl = DELIVERY_METHODS_PATH + "?id=erliPaczkomat&cod=false&vendor=inpost";
        server.stubFor(get(urlEqualTo(expectedUrl)).willReturn(okJson("[]")));

        dictionaries().deliveryMethods(DeliveryMethodQuery.builder()
                .id(DeliveryMethodId.of("erliPaczkomat"))
                .cashOnDelivery(false)
                .vendor(DeliveryVendor.INPOST)
                .build());

        server.verify(getRequestedFor(urlEqualTo(expectedUrl))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void omitsTheQueryStringEntirelyForAnEmptyQuery() {
        server.stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH)).willReturn(okJson("[]")));

        dictionaries().deliveryMethods(DeliveryMethodQuery.none());

        server.verify(getRequestedFor(urlEqualTo(DELIVERY_METHODS_PATH)));
    }

    @Test
    void substitutesAndEncodesThePriceListPathSegment() {
        String expectedUrl = DELIVERY_METHODS_PATH + "/cennik%20letni";
        server.stubFor(get(urlEqualTo(expectedUrl)).willReturn(okJson("[]")));

        dictionaries().deliveryMethods(PriceListName.of("cennik letni"), DeliveryMethodQuery.none());

        server.verify(getRequestedFor(urlEqualTo(expectedUrl)));
    }

    @Test
    void sendsShippingMethodFiltersAsAQueryString() {
        String expectedUrl = "/dictionaries/shippingMethods?groupId=erliPaczkomat&operator=INPOST&cod=true";
        server.stubFor(get(urlEqualTo(expectedUrl)).willReturn(okJson("[]")));

        dictionaries().shippingMethods(ShippingMethodQuery.builder()
                .groupId("erliPaczkomat")
                .operator(ShippingOperator.INPOST)
                .cashOnDelivery(true)
                .build());

        server.verify(getRequestedFor(urlEqualTo(expectedUrl)));
    }

    @Test
    void sendsResponsiblePartyFiltersAsAQueryString() {
        String expectedUrl = "/dictionaries/responsibleProducers?name=Importer";
        server.stubFor(get(urlEqualTo(expectedUrl)).willReturn(okJson("[]")));

        dictionaries().responsibleProducers(ResponsiblePartyQuery.byName("Importer"));

        server.verify(getRequestedFor(urlEqualTo(expectedUrl)));
    }

    @Test
    void sendsAttachmentFiltersAsAQueryString() {
        String expectedUrl = "/dictionaries/attachments?id=7&kind=userManual&name=instrukcja";
        server.stubFor(get(urlEqualTo(expectedUrl)).willReturn(okJson("[]")));

        dictionaries().attachments(AttachmentQuery.builder()
                .id(7L)
                .kind(AttachmentKind.USER_MANUAL)
                .name("instrukcja")
                .build());

        server.verify(getRequestedFor(urlEqualTo(expectedUrl)));
    }

    @Test
    void decodesTheBareStringArrayOfTheVendorDictionary() {
        server.stubFor(get(urlEqualTo("/dictionaries/deliveryVendors"))
                .willReturn(okJson("[\"inpost\",\"dhl\",\"selfPickup\"]")));

        List<DeliveryVendor> vendors = dictionaries().deliveryVendors();

        assertEquals(List.of(DeliveryVendor.INPOST, DeliveryVendor.DHL, DeliveryVendor.SELF_PICKUP), vendors);
    }

    @Test
    void sendsTheCategoryIdInTheAttributeSearchBody() {
        server.stubFor(post(urlEqualTo("/dictionaries/attributes/_search")).willReturn(okJson("[]")));

        dictionaries().attributes(CategoryId.of("4"));

        server.verify(postRequestedFor(urlEqualTo("/dictionaries/attributes/_search"))
                .withHeader(HEADER_CONTENT_TYPE, equalTo(MEDIA_TYPE_JSON))
                .withRequestBody(equalToJson("{\"categoryId\":4}")));
    }

    @Test
    void sendsTheCategoryIdInTheAttributeValuesSearchBody() {
        server.stubFor(post(urlEqualTo("/dictionaries/attributeValues/_search")).willReturn(okJson("[]")));

        dictionaries().attributeValues(CategoryId.of("4"));

        server.verify(postRequestedFor(urlEqualTo("/dictionaries/attributeValues/_search"))
                .withRequestBody(equalToJson("{\"categoryId\":4}")));
    }

    @Test
    void sendsTheCreateResponsiblePersonBody() {
        server.stubFor(post(urlEqualTo(RESPONSIBLE_PERSONS_PATH)).willReturn(okJson(responsiblePartyJson())));

        dictionaries().createResponsiblePerson(NewResponsibleParty.builder()
                .name("Importer PL")
                .idempotenceKey("imp-001")
                .properName("Importer Sp. z o.o.")
                .country(CountryCode.PL)
                .address("ul. Przykładowa 1")
                .postalCode("00-001")
                .city("Warszawa")
                .email("kontakt@example.com")
                .build());

        // The optional phone/source are absent, not null: the API rejects an explicit null.
        server.verify(postRequestedFor(urlEqualTo(RESPONSIBLE_PERSONS_PATH))
                .withHeader(HEADER_CONTENT_TYPE, equalTo(MEDIA_TYPE_JSON))
                .withRequestBody(equalToJson("""
                        {"name":"Importer PL","idempotenceKey":"imp-001",
                         "properName":"Importer Sp. z o.o.","country":"pl",
                         "address":"ul. Przykładowa 1","postalCode":"00-001","city":"Warszawa",
                         "email":"kontakt@example.com"}""")));
    }

    @Test
    void patchesOnlyTheResponsiblePartyFieldsThatWereSet() {
        server.stubFor(patch(urlEqualTo("/dictionaries/responsiblePersons/7"))
                .willReturn(okJson(responsiblePartyJson())));

        dictionaries().updateResponsiblePerson(7L, ResponsiblePartyUpdate.builder(CountryCode.PL).city("Kraków").build());

        // country travels on every patch even though only the city changed: the API demands it.
        server.verify(patchRequestedFor(urlEqualTo("/dictionaries/responsiblePersons/7"))
                .withRequestBody(equalToJson("{\"country\":\"pl\",\"city\":\"Kraków\"}")));
    }

    @Test
    void deletesAResponsibleProducerById() {
        server.stubFor(delete(urlEqualTo("/dictionaries/responsibleProducers/9"))
                .willReturn(aResponse().withStatus(200)));

        dictionaries().deleteResponsibleProducer(9L);

        server.verify(deleteRequestedFor(urlEqualTo("/dictionaries/responsibleProducers/9")));
    }

    @Test
    void sendsTheCreateAttachmentBody() {
        server.stubFor(post(urlEqualTo("/dictionaries/attachment")).willReturn(okJson(attachmentJson())));

        dictionaries().createAttachment(NewAttachment.builder()
                .kind(AttachmentKind.USER_MANUAL)
                .name("Instrukcja")
                .originalName("instrukcja.pdf")
                .filePath("shop/100007/instrukcja.pdf")
                .markets(List.of(Market.POLAND, Market.GERMANY))
                .build());

        server.verify(postRequestedFor(urlEqualTo("/dictionaries/attachment"))
                .withRequestBody(equalToJson("""
                        {"kind":"userManual","name":"Instrukcja","originalName":"instrukcja.pdf",
                         "filePath":"shop/100007/instrukcja.pdf","markets":["pl","de"]}""")));
    }

    @Test
    void patchesOnlyTheAttachmentFieldsThatWereSet() {
        server.stubFor(patch(urlEqualTo("/dictionaries/attachment")).willReturn(okJson(attachmentJson())));

        dictionaries().updateAttachment(AttachmentUpdate.builder(7L).name("Nowa nazwa").build());

        server.verify(patchRequestedFor(urlEqualTo("/dictionaries/attachment"))
                .withRequestBody(equalToJson("{\"id\":7,\"name\":\"Nowa nazwa\"}")));
    }

    @Test
    void sendsTheAttachBodyWithTheAttachAction() {
        server.stubFor(patch(urlEqualTo(ATTACH_PATH))
                .willReturn(okJson("{\"ok\":true,\"updated\":[11,12],\"errors\":[]}")));

        ProductAttachmentResult result = dictionaries().attachProducts(7L, List.of(11L, 12L));

        server.verify(patchRequestedFor(urlEqualTo(ATTACH_PATH))
                .withRequestBody(equalToJson("{\"action\":\"attach\",\"attachmentId\":7,\"productIds\":[11,12]}")));
        assertTrue(result.isComplete());
    }

    @Test
    void sendsTheDetachBodyWithTheDetachAction() {
        server.stubFor(patch(urlEqualTo(DETACH_PATH))
                .willReturn(okJson("{\"ok\":true,\"updated\":[11],\"errors\":[]}")));

        dictionaries().detachProducts(7L, List.of(11L));

        server.verify(patchRequestedFor(urlEqualTo(DETACH_PATH))
                .withRequestBody(equalToJson("{\"action\":\"detach\",\"attachmentId\":7,\"productIds\":[11]}")));
    }

    @Test
    void surfacesThePerProductFailuresTheApiReportsWithHttp200() {
        // The API answers 200 with ok:false when a product could not be attached; a caller that only
        // watched for an exception would believe an attach that changed nothing had succeeded.
        server.stubFor(patch(urlEqualTo(ATTACH_PATH)).willReturn(okJson("""
                {"ok":false,"updated":[],
                 "errors":[{"productId":999999999,"error":"NotFoundFailure: product not found"}]}""")));

        ProductAttachmentResult result = dictionaries().attachProducts(7L, List.of(999999999L));

        assertFalse(result.isComplete());
        assertEquals(999999999L, result.errors().get(0).productId().orElseThrow());
    }

    @Test
    void sendsTheAttachmentIdsAsABareArrayOnDelete() {
        server.stubFor(delete(urlEqualTo(ATTACHMENTS_PATH))
                .willReturn(okJson("{\"removedAttachments\":[73],\"errors\":[]}")));

        AttachmentRemoval removal = dictionaries().deleteAttachments(List.of(73L));

        server.verify(deleteRequestedFor(urlEqualTo(ATTACHMENTS_PATH))
                .withHeader(HEADER_CONTENT_TYPE, equalTo(MEDIA_TYPE_JSON))
                .withRequestBody(equalToJson("[73]")));
        assertTrue(removal.isComplete());
        assertEquals(List.of(73L), removal.removedAttachmentIds());
    }

    @Test
    void reportsAPartialAttachmentRemoval() {
        server.stubFor(delete(urlEqualTo(ATTACHMENTS_PATH))
                .willReturn(okJson("{\"removedAttachments\":[73],\"errors\":[\"74 is in use\"]}")));

        AttachmentRemoval removal = dictionaries().deleteAttachments(List.of(73L, 74L));

        assertFalse(removal.isComplete());
        assertEquals(1, removal.errors().size());
    }

    /**
     * The person and producer operations differ only in their path constant — the classic copy-paste
     * hazard — so every one of the eight is pinned to the resource it belongs to.
     */
    @Test
    void routesEveryResponsiblePartyOperationToItsOwnResource() {
        server.stubFor(get(urlEqualTo(RESPONSIBLE_PERSONS_PATH)).willReturn(okJson("[]")));
        server.stubFor(get(urlEqualTo(RESPONSIBLE_PRODUCERS_PATH)).willReturn(okJson("[]")));
        server.stubFor(post(urlEqualTo(RESPONSIBLE_PRODUCERS_PATH)).willReturn(okJson(responsiblePartyJson())));
        server.stubFor(patch(urlEqualTo(RESPONSIBLE_PRODUCERS_PATH + "/7")).willReturn(okJson(responsiblePartyJson())));
        server.stubFor(delete(urlEqualTo(RESPONSIBLE_PERSONS_PATH + "/7")).willReturn(aResponse().withStatus(200)));

        DictionariesAccessImpl dictionaries = dictionaries();
        dictionaries.responsiblePersons(ResponsiblePartyQuery.none());
        dictionaries.responsibleProducers(ResponsiblePartyQuery.none());
        dictionaries.createResponsibleProducer(newParty());
        dictionaries.updateResponsibleProducer(7L, ResponsiblePartyUpdate.builder(CountryCode.PL).city("Gdańsk").build());
        dictionaries.deleteResponsiblePerson(7L);

        server.verify(getRequestedFor(urlEqualTo(RESPONSIBLE_PERSONS_PATH)));
        server.verify(getRequestedFor(urlEqualTo(RESPONSIBLE_PRODUCERS_PATH)));
        server.verify(postRequestedFor(urlEqualTo(RESPONSIBLE_PRODUCERS_PATH)));
        server.verify(patchRequestedFor(urlEqualTo(RESPONSIBLE_PRODUCERS_PATH + "/7")));
        server.verify(deleteRequestedFor(urlEqualTo(RESPONSIBLE_PERSONS_PATH + "/7")));
    }

    @Test
    void requestsTheBillingEntryTypeDictionary() {
        server.stubFor(get(urlEqualTo("/dictionaries/billingEntryTypes"))
                .willReturn(okJson("[{\"type\":\"COMM\",\"description\":\"naliczenie prowizji\"}]")));

        assertEquals("COMM", dictionaries().billingEntryTypes().get(0).type());

        server.verify(getRequestedFor(urlEqualTo("/dictionaries/billingEntryTypes")));
    }

    @Test
    void rejectsAnEmptyProductListBeforeCallingTheApi() {
        assertThrows(IllegalArgumentException.class, () -> dictionaries().attachProducts(7L, List.of()));

        assertTrue(server.getAllServeEvents().isEmpty(), "no request should have been sent");
    }

    @Test
    void walksTheCategoryCursorAcrossPagesAndStopsOnAShortPage() {
        // Page 1 is full, so the SDK asks for page 2 using the last id as the `after` cursor;
        // page 2 is short, which ends the walk.
        server.stubFor(post(urlEqualTo(CATEGORY_SEARCH_PATH))
                .withRequestBody(equalToJson("{\"limit\":200}"))
                .willReturn(okJson(fullCategoryPage())));
        server.stubFor(post(urlEqualTo(CATEGORY_SEARCH_PATH))
                .withRequestBody(equalToJson("{\"limit\":200,\"after\":199}"))
                .willReturn(okJson("[{\"id\":200,\"name\":\"Ostatnia\",\"leaf\":true,\"breadcrumb\":[]}]")));

        List<Category> categories = dictionaries().categories().toList();

        assertEquals(CATEGORY_PAGE_SIZE + 1, categories.size());
        assertEquals(CategoryId.of("200"), categories.get(CATEGORY_PAGE_SIZE).id());
        server.verify(2, postRequestedFor(urlEqualTo(CATEGORY_SEARCH_PATH)));
    }

    @Test
    void omitsTheAfterCursorOnTheFirstCategoryPage() {
        // The API rejects an explicit "after": null with a 400 (observed live), so the first page must
        // not carry the key at all.
        server.stubFor(post(urlEqualTo(CATEGORY_SEARCH_PATH)).willReturn(okJson("[]")));

        dictionaries().categories().toList();

        server.verify(postRequestedFor(urlEqualTo(CATEGORY_SEARCH_PATH))
                .withRequestBody(equalToJson("{\"limit\":200}")));
    }

    @Test
    void fetchesOnlyTheFirstCategoryPageWhenTheStreamShortCircuits() {
        server.stubFor(post(urlEqualTo(CATEGORY_SEARCH_PATH)).willReturn(okJson(fullCategoryPage())));

        Category first = dictionaries().categories().findFirst().orElseThrow();

        assertEquals(CategoryId.of("0"), first.id());
        // Laziness: a full first page would otherwise have triggered a second request.
        server.verify(1, postRequestedFor(urlEqualTo(CATEGORY_SEARCH_PATH)));
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
                        .withHeader(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
                        .withBody(OBSERVED_400_BODY)));

        ErliException failure = assertThrows(ErliException.class, () -> dictionaries().deliveryMethods());

        assertEquals(expectedExceptionName, failure.getClass().getSimpleName());
    }

    @Test
    void preservesTheRicherThanSpecErrorPayloadObservedLive() {
        server.stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .willReturn(aResponse().withStatus(400)
                        .withHeader(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
                        .withBody(OBSERVED_400_BODY)));

        ErliValidationException failure =
                assertThrows(ErliValidationException.class, () -> dictionaries().deliveryMethods());

        assertEquals("699t9FoRLPuu1", failure.details().traceId());
        assertEquals("validation", failure.details().failureType());
        assertEquals("Problem z walidacją, sprawdź pola", failure.details().polishMessage());
        assertTrue(failure.details().rawBody().contains("categoryId"), failure.details().rawBody());
    }

    @Test
    void mapsANonJsonErrorBodyAndKeepsItRaw() {
        server.stubFor(get(urlEqualTo(DELIVERY_METHODS_PATH))
                .willReturn(aResponse().withStatus(500).withBody("<html>gateway blew up</html>")));

        ErliServerException failure =
                assertThrows(ErliServerException.class, () -> dictionaries().deliveryMethods());

        assertTrue(failure.details().rawBody().contains("gateway blew up"), failure.details().rawBody());
    }

    private static NewResponsibleParty newParty() {
        return NewResponsibleParty.builder()
                .name("Importer PL")
                .idempotenceKey("imp-001")
                .properName("Importer Sp. z o.o.")
                .country(CountryCode.PL)
                .address("ul. Przykładowa 1")
                .postalCode("00-001")
                .city("Warszawa")
                .email("kontakt@example.com")
                .build();
    }

    /** The single object the create/update endpoints really return — the spec declares an array. */
    private static String responsiblePartyJson() {
        return """
                {"id":7,"idempotenceKey":"imp-001","name":"Importer PL","properName":"Importer Sp. z o.o.",
                 "address":"ul. Przykładowa 1","city":"Kraków","postalCode":"00-001","country":"pl",
                 "email":"kontakt@example.com","source":"api"}""";
    }

    private static String attachmentJson() {
        return """
                {"id":7,"shopId":100007,"version":1,"name":"Instrukcja",
                 "originalName":"instrukcja.pdf","filePath":"shop/100007/instrukcja.pdf",
                 "kind":"userManual","attachedProductIds":[],
                 "created":{"time":"2026-07-25T10:00:00+02:00","user":{"userId":"u-1"}}}""";
    }

    /** A page of exactly the SDK's category page size, so the cursor advances. */
    private static String fullCategoryPage() {
        StringBuilder page = new StringBuilder("[");
        for (int id = 0; id < CATEGORY_PAGE_SIZE; id++) {
            page.append(id == 0 ? "" : ",")
                    .append("{\"id\":").append(id)
                    .append(",\"name\":\"Kategoria ").append(id)
                    .append("\",\"leaf\":false,\"breadcrumb\":[]}");
        }
        return page.append("]").toString();
    }
}
