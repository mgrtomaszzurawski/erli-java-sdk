package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.error.ErliApiException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliErrorDetails;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorMapperTest {

    // The real body observed live on the sandbox 401 — richer than the spec Error schema:
    // failureType "security" (not in the spec enum), httpCode + payload present, no traceId.
    private static final String OBSERVED_401_BODY = """
            {"failureType":"security","message":"Invalid API key","httpCode":401,\
            "polishMessage":"Nieprawidłowy klucz API","payload":"Invalid API key","spanId":"span-abc"}""";

    private final ErrorMapper mapper = new ErrorMapper(new JsonCodec());

    @Test
    void mapsObservedRicherThanSpec401ToAuthWithAllFields() {
        ErliApiException exception = mapper.toException(401, OBSERVED_401_BODY);

        assertInstanceOf(ErliAuthException.class, exception);
        ErliErrorDetails details = exception.details();
        assertEquals(401, details.httpStatus());
        assertEquals(Integer.valueOf(401), details.bodyHttpCode());
        assertEquals("security", details.failureType());
        assertEquals("Invalid API key", details.message());
        assertEquals("Nieprawidłowy klucz API", details.polishMessage());
        assertEquals("span-abc", details.spanId());
        assertEquals("Invalid API key", details.payload());
        assertNull(details.traceId(), "the observed body carried no traceId");
    }

    @Test
    void selectsRemediationGroupByHttpStatus() {
        assertInstanceOf(ErliAuthException.class, mapper.toException(403, "{}"));
        assertInstanceOf(ErliNotFoundException.class, mapper.toException(404, "{}"));
        assertInstanceOf(ErliValidationException.class, mapper.toException(400, "{}"));
        assertInstanceOf(ErliValidationException.class, mapper.toException(422, "{}"));
        assertInstanceOf(ErliServerException.class, mapper.toException(500, "{}"));
        assertInstanceOf(ErliServerException.class, mapper.toException(503, "{}"));
    }

    @Test
    void toleratesNonJsonBodyByKeepingRawAndLeavingFieldsNull() {
        ErliApiException exception = mapper.toException(503, "<html>Bad Gateway</html>");

        ErliErrorDetails details = exception.details();
        assertEquals(503, details.httpStatus());
        assertEquals("<html>Bad Gateway</html>", details.rawBody());
        assertNull(details.message());
        assertNull(details.failureType());
    }

    @Test
    void carriesTraceIdWhenPresent() {
        ErliApiException exception = mapper.toException(404, "{\"traceId\":\"trace-xyz\",\"message\":\"nope\"}");
        assertEquals("trace-xyz", exception.details().traceId());
        assertTrue(exception.getMessage().contains("trace-xyz"));
    }

    @Test
    void preservesScalarPayload() {
        ErliApiException exception = mapper.toException(401,
                "{\"failureType\":\"security\",\"payload\":\"Invalid API key\"}");
        assertEquals("Invalid API key", exception.details().payload());
    }

    @Test
    void preservesObjectPayloadAsJsonInsteadOfDroppingIt() {
        // CORE-8: validation errors carry per-field detail as an object payload; a plain-text
        // extraction would silently drop it. It is kept as its compact JSON string.
        ErliApiException exception = mapper.toException(400,
                "{\"failureType\":\"validation\",\"payload\":{\"details\":{\"types\":\"unknown filter\"}}}");

        String payload = exception.details().payload();
        assertTrue(payload.contains("details"), "object payload lost");
        assertTrue(payload.contains("unknown filter"), "per-field detail lost");
    }
}
