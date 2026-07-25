package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Full delivery-methods slice through {@link ErliClient}: accessor → impl → query → transport → mapper. */
class DictionariesSliceTest {

    private static final String PATH = "/dictionaries/deliveryMethods";
    private static final String DELIVERY_METHODS_JSON =
            "[{\"id\":\"courier-1\",\"name\":\"Kurier\",\"cod\":true,\"vendor\":\"inpost\"},"
            + "{\"id\":\"parcel-1\",\"name\":\"Paczkomat\",\"cod\":false,\"vendor\":\"inpost\"}]";

    private WireMockServer server;
    private ErliClient client;

    @BeforeEach
    void start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        client = ErliClient.builder().baseUrl(server.baseUrl()).apiKey(ApiKey.of("test-key")).build();
    }

    @AfterEach
    void stop() {
        client.close();
        server.stop();
    }

    @Test
    void mapsTheDeliveryMethodListAndSendsNoQueryWhenUnfiltered() {
        server.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(DELIVERY_METHODS_JSON)));

        List<DeliveryMethod> methods = client.dictionaries().deliveryMethods();

        assertEquals(2, methods.size());
        assertEquals("courier-1", methods.get(0).id().value());
        assertEquals("Kurier", methods.get(0).name());
        assertEquals(DeliveryVendor.INPOST, methods.get(0).vendor());
        assertFalse(methods.get(1).cashOnDelivery());
        // urlEqualTo matches the full URL, so this also asserts no query string was appended.
        server.verify(getRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void appliesSetFiltersAndOmitsUnsetOnes() {
        server.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson(DELIVERY_METHODS_JSON)));

        client.dictionaries().deliveryMethods(DeliveryMethodQuery.builder()
                .cashOnDelivery(true)
                .vendor(DeliveryVendor.INPOST)
                .build());

        server.verify(getRequestedFor(urlPathEqualTo(PATH))
                .withQueryParam("cod", equalTo("true"))
                .withQueryParam("vendor", equalTo("inpost"))
                .withQueryParam("id", absent()));
    }
}
