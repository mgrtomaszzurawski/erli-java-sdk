package io.github.mgrtomaszzurawski.erli.internal.client.inbox;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.inbox.InboxAccess;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Message;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageId;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageQuery;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageType;
import io.github.mgrtomaszzurawski.erli.domain.inbox.ReadReceipt;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verify-on-write coverage of the three {@code /inbox} operations, plus the bucket's mandatory
 * error-path table ({@code TESTING.md}).
 */
class InboxAccessImplTest {

    private static final String INBOX_PATH = "/inbox";
    private static final String SEARCH_PATH = "/inbox/_search";
    private static final String MARK_READ_PATH = "/inbox/mark-read";
    private static final String TEST_KEY = "test-key";
    private static final String USER_AGENT = "erli-java-sdk/test";
    private static final String MESSAGE_ID = "5f9e1b3b0f0b9b0001c3e0b1";
    private static final String VALIDATION_ERROR_FIXTURE = "/fixtures/inbox/observed-search-validation-error.json";

    private static final String SYNC_MESSAGE_JSON = """
            [{"id":"5f9e1b3b0f0b9b0001c3e0b1","shopId":100007,"created":"2026-07-25T12:00:00Z",
              "read":false,"type":"productsNeedSync",
              "payload":{"id":"hash-1","externalProductIds":["SKU-1"],"fields":[]}}]""";
    private static final String OBSERVED_401_BODY =
            "{\"failureType\":\"security\",\"message\":\"Invalid API key\",\"httpCode\":401,\"spanId\":\"span-1\"}";

    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_SERVER_ERROR = 503;
    private static final int MAX_ATTEMPTS = 1;
    private static final int EXPECTED_MARKED_COUNT = 7;

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

    private InboxAccess inbox() {
        JsonCodec codec = new JsonCodec();
        HttpRuntime runtime = new HttpRuntime(
                HttpClient.newHttpClient(),
                server.baseUrl(),
                ApiKey.of(TEST_KEY),
                RetryPolicy.builder()
                        .maxAttempts(MAX_ATTEMPTS)
                        .baseDelay(Duration.ofMillis(1))
                        .maxDelay(Duration.ofMillis(2))
                        .randomGenerator(new Random(0))
                        .build(),
                USER_AGENT,
                Duration.ofSeconds(5),
                codec,
                new ErrorMapper(codec));
        return new InboxAccessImpl(runtime, codec);
    }

    @Test
    void fetchesUnreadMessages() {
        server.stubFor(get(urlEqualTo(INBOX_PATH)).willReturn(okJson(SYNC_MESSAGE_JSON)));

        List<Message> unread = inbox().unread();

        assertEquals(1, unread.size());
        assertEquals(MessageType.PRODUCTS_NEED_SYNC, unread.get(0).type());
        assertTrue(unread.get(0).productsSyncEvent().orElseThrow().wholeProduct());
        server.verify(getRequestedFor(urlEqualTo(INBOX_PATH))
                .withHeader("Authorization", equalTo("Bearer " + TEST_KEY))
                .withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void treatsAnEmptyInboxAsNoMessages() {
        server.stubFor(get(urlEqualTo(INBOX_PATH)).willReturn(okJson("[]")));

        assertEquals(List.of(), inbox().unread());
    }

    @Test
    void sendsTheTypeFilterOnSearch() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson(SYNC_MESSAGE_JSON)));

        List<Message> found = inbox().search(MessageQuery.ofTypes(
                Set.of(MessageType.PRODUCTS_NEED_SYNC, MessageType.ORDER_CREATED)));

        assertEquals(1, found.size());
        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("{\"types\":[\"orderCreated\",\"productsNeedSync\"]}")));
    }

    /** An unfiltered search must send the schema's documented default body, not {@code {"types":[]}}. */
    @Test
    void sendsAnEmptyBodyWhenSearchingWithoutAFilter() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(okJson("[]")));

        inbox().search(MessageQuery.all());

        server.verify(postRequestedFor(urlEqualTo(SEARCH_PATH)).withRequestBody(equalToJson("{}")));
    }

    @Test
    void marksMessagesReadUpToOneIdAndReturnsTheCount() {
        server.stubFor(post(urlEqualTo(MARK_READ_PATH))
                .willReturn(okJson(String.valueOf(EXPECTED_MARKED_COUNT))));

        int marked = inbox().markRead(ReadReceipt.upTo(MessageId.of(MESSAGE_ID)));

        assertEquals(EXPECTED_MARKED_COUNT, marked);
        server.verify(postRequestedFor(urlEqualTo(MARK_READ_PATH))
                .withRequestBody(equalToJson("{\"lastMessageId\":\"" + MESSAGE_ID + "\"}")));
    }

    @Test
    void marksAnExplicitSetOfMessagesRead() {
        server.stubFor(post(urlEqualTo(MARK_READ_PATH)).willReturn(okJson("2")));

        int marked = inbox().markRead(ReadReceipt.exactly(
                List.of(MessageId.of(MESSAGE_ID), MessageId.of("5f9e1b3b0f0b9b0001c3e0a0"))));

        assertEquals(2, marked);
        server.verify(postRequestedFor(urlEqualTo(MARK_READ_PATH)).withRequestBody(equalToJson(
                "{\"ids\":[\"" + MESSAGE_ID + "\",\"5f9e1b3b0f0b9b0001c3e0a0\"]}")));
    }

    // --- mandatory error-path table (TESTING.md) ---------------------------------------------------

    @Test
    void mapsUnauthorizedToTheAuthRemediation() {
        server.stubFor(get(urlEqualTo(INBOX_PATH))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody(OBSERVED_401_BODY)));

        ErliAuthException thrown = assertThrows(ErliAuthException.class, () -> inbox().unread());

        assertEquals("security", thrown.details().failureType());
    }

    @Test
    void mapsNotFoundToTheNotFoundRemediation() {
        server.stubFor(post(urlEqualTo(MARK_READ_PATH))
                .willReturn(aResponse().withStatus(HTTP_NOT_FOUND).withBody("{\"message\":\"no such message\"}")));

        assertThrows(ErliNotFoundException.class,
                () -> inbox().markRead(ReadReceipt.upTo(MessageId.of(MESSAGE_ID))));
    }

    /**
     * Asserted against the <strong>real</strong> 400 the sandbox returned on 2026-07-25 for
     * {@code {"types":[]}} (fixture provenance, {@code TESTING.md}). It diverges from the spec's
     * {@code Error} schema in three ways the mapping must tolerate: a {@code name} field, a
     * {@code failureType} of {@code "validation"} (the spec enum lists only {@code failure|conflict}),
     * and an <em>object</em> {@code payload} where the previously observed 401 had a string.
     */
    @Test
    void mapsTheRealObservedValidationErrorBody() {
        server.stubFor(post(urlEqualTo(SEARCH_PATH)).willReturn(
                aResponse().withStatus(HTTP_BAD_REQUEST).withBody(readFixture(VALIDATION_ERROR_FIXTURE))));

        ErliValidationException thrown =
                assertThrows(ErliValidationException.class, () -> inbox().search(MessageQuery.all()));

        assertEquals("validation", thrown.details().failureType());
        assertEquals("ValidationFailure", thrown.details().name());
        assertEquals("lmtps6H4szmP5", thrown.details().traceId());
        assertEquals("lmtps6H4szmP5", thrown.details().spanId());
        assertTrue(thrown.details().polishMessage().contains("walidacj"), thrown.details().polishMessage());
        // The object-shaped payload degrades to null rather than failing the decode; the raw body keeps
        // the per-field validation details until core extracts them (BACKLOG CORE-4).
        assertNull(thrown.details().payload());
        assertTrue(thrown.details().rawBody().contains("does not contain 1 required value"));
    }

    @Test
    void mapsServerErrorToTheServerRemediation() {
        server.stubFor(get(urlEqualTo(INBOX_PATH)).willReturn(aResponse().withStatus(HTTP_SERVER_ERROR)));

        assertThrows(ErliServerException.class, () -> inbox().unread());
    }

    /** A non-JSON error body must still map by status and keep the raw text for diagnosis. */
    @Test
    void mapsANonJsonErrorBodyAndKeepsIt() {
        server.stubFor(get(urlEqualTo(INBOX_PATH))
                .willReturn(aResponse().withStatus(HTTP_UNAUTHORIZED).withBody("<html>denied</html>")));

        ErliAuthException thrown = assertThrows(ErliAuthException.class, () -> inbox().unread());

        assertEquals("<html>denied</html>", thrown.details().rawBody());
    }

    private static String readFixture(String resource) {
        try (InputStream stream = InboxAccessImplTest.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test fixture " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read test fixture " + resource, failure);
        }
    }
}
