package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Category;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.CountryCode;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethodFilter;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.NewResponsibleParty;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.PriceListName;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ResponsiblePartyFilter;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethodFilter;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingOperator;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-level tests for the dictionary endpoints beyond the starter slice: every test asserts the
 * request the SDK produced (path, query string, body), not only the response it decoded.
 */
class DictionaryEndpointsTest {

    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String MEDIA_TYPE_JSON = "application/json";
    private static final String CATEGORY_SEARCH_PATH = "/dictionaries/category/_search";
    private static final String RESPONSIBLE_PERSONS_PATH = "/dictionaries/responsiblePersons";

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
    void sendsDeliveryMethodFiltersAsAQueryString() {
        server.stubFor(get(urlEqualTo("/dictionaries/deliveryMethods?id=erliPaczkomat&cod=false&vendor=inpost"))
                .willReturn(okJson("[]")));

        dictionaries().deliveryMethods(DeliveryMethodFilter.builder()
                .id(DeliveryMethodId.of("erliPaczkomat"))
                .cashOnDelivery(false)
                .vendor(DeliveryVendor.INPOST)
                .build());

        server.verify(getRequestedFor(
                urlEqualTo("/dictionaries/deliveryMethods?id=erliPaczkomat&cod=false&vendor=inpost")));
    }

    @Test
    void omitsTheQueryStringEntirelyForAnEmptyFilter() {
        server.stubFor(get(urlEqualTo("/dictionaries/deliveryMethods")).willReturn(okJson("[]")));

        dictionaries().deliveryMethods(DeliveryMethodFilter.all());

        server.verify(getRequestedFor(urlEqualTo("/dictionaries/deliveryMethods")));
    }

    @Test
    void substitutesAndEncodesThePriceListPathSegment() {
        server.stubFor(get(urlEqualTo("/dictionaries/deliveryMethods/cennik%20letni")).willReturn(okJson("[]")));

        dictionaries().deliveryMethods(PriceListName.of("cennik letni"), DeliveryMethodFilter.all());

        server.verify(getRequestedFor(urlEqualTo("/dictionaries/deliveryMethods/cennik%20letni")));
    }

    @Test
    void sendsShippingMethodFiltersAsAQueryString() {
        String expectedUrl = "/dictionaries/shippingMethods?groupId=erliPaczkomat&operator=INPOST&cod=true";
        server.stubFor(get(urlEqualTo(expectedUrl)).willReturn(okJson("[]")));

        dictionaries().shippingMethods(ShippingMethodFilter.builder()
                .groupId("erliPaczkomat")
                .operator(ShippingOperator.INPOST)
                .cashOnDelivery(true)
                .build());

        server.verify(getRequestedFor(urlEqualTo(expectedUrl)));
    }

    @Test
    void sendsResponsiblePartyFiltersAsAQueryString() {
        server.stubFor(get(urlEqualTo("/dictionaries/responsibleProducers?name=Importer")).willReturn(okJson("[]")));

        dictionaries().responsibleProducers(ResponsiblePartyFilter.byName("Importer"));

        server.verify(getRequestedFor(urlEqualTo("/dictionaries/responsibleProducers?name=Importer")));
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
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JSON))
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
        server.stubFor(post(urlEqualTo(RESPONSIBLE_PERSONS_PATH)).willReturn(okJson("[]")));

        dictionaries().createResponsiblePerson(NewResponsibleParty.builder()
                .name("Importer PL")
                .idempotenceKey("imp-001")
                .properName("Importer Sp. z o.o.")
                .country(CountryCode.POLAND)
                .address("ul. Przykładowa 1")
                .postalCode("00-001")
                .city("Warszawa")
                .email("kontakt@example.com")
                .build());

        server.verify(postRequestedFor(urlEqualTo(RESPONSIBLE_PERSONS_PATH))
                .withHeader("Content-Type", equalTo(MEDIA_TYPE_JSON))
                .withRequestBody(equalToJson("""
                        {"name":"Importer PL","idempotenceKey":"imp-001",
                         "properName":"Importer Sp. z o.o.","country":"pl",
                         "address":"ul. Przykładowa 1","postalCode":"00-001","city":"Warszawa",
                         "email":"kontakt@example.com","phone":null,"source":null}""")));
    }

    @Test
    void walksTheCategoryCursorAcrossPagesAndStopsOnAShortPage() {
        // Page 1 is full (the stub returns the configured page size), so the SDK asks for page 2 using
        // the last id as the `after` cursor; page 2 is short, which ends the walk.
        server.stubFor(post(urlEqualTo(CATEGORY_SEARCH_PATH))
                .withRequestBody(equalToJson("{\"limit\":200,\"after\":null}", true, true))
                .willReturn(okJson(fullCategoryPage())));
        server.stubFor(post(urlEqualTo(CATEGORY_SEARCH_PATH))
                .withRequestBody(equalToJson("{\"limit\":200,\"after\":199}", true, true))
                .willReturn(okJson("[{\"id\":200,\"name\":\"Ostatnia\",\"leaf\":true,\"breadcrumb\":[]}]")));

        List<Category> categories = dictionaries().categories().toList();

        assertEquals(201, categories.size());
        assertEquals(CategoryId.of("200"), categories.get(200).id());
        server.verify(2, postRequestedFor(urlEqualTo(CATEGORY_SEARCH_PATH)));
    }

    @Test
    void fetchesOnlyTheFirstCategoryPageWhenTheStreamShortCircuits() {
        server.stubFor(post(urlEqualTo(CATEGORY_SEARCH_PATH)).willReturn(okJson(fullCategoryPage())));

        Category first = dictionaries().categories().findFirst().orElseThrow();

        assertEquals(CategoryId.of("0"), first.id());
        // Laziness: a full first page would otherwise have triggered a second request.
        server.verify(1, postRequestedFor(urlEqualTo(CATEGORY_SEARCH_PATH)));
    }

    @Test
    void stopsTheCategoryWalkOnAnEmptyPage() {
        server.stubFor(post(urlEqualTo(CATEGORY_SEARCH_PATH)).willReturn(okJson("[]")));

        assertTrue(dictionaries().categories().toList().isEmpty());

        server.verify(1, postRequestedFor(urlEqualTo(CATEGORY_SEARCH_PATH)));
    }

    /** A page of exactly the SDK's category page size, so the cursor advances. */
    private static String fullCategoryPage() {
        StringBuilder page = new StringBuilder("[");
        for (int id = 0; id < 200; id++) {
            page.append(id == 0 ? "" : ",")
                    .append("{\"id\":").append(id)
                    .append(",\"name\":\"Kategoria ").append(id)
                    .append("\",\"leaf\":false,\"breadcrumb\":[]}");
        }
        return page.append("]").toString();
    }
}
