package io.github.mgrtomaszzurawski.erli.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.error.ErliApiException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliErrorDetails;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliServerException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;

/**
 * Maps a non-success HTTP response to the remediation-grouped exception. Selection is driven by the
 * transport HTTP status (the reliable signal); the body enriches {@link ErliErrorDetails} but is not
 * required to be present or well-formed. Tolerant of the live API's richer-than-spec error payload
 * ({@code failureType:"security"}, {@code httpCode}, {@code payload}, missing {@code traceId}).
 * Internal: never exported.
 */
public final class ErrorMapper {

    private static final String FIELD_HTTP_CODE = "httpCode";
    private static final String FIELD_FAILURE_TYPE = "failureType";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_POLISH_MESSAGE = "polishMessage";
    private static final String FIELD_TRACE_ID = "traceId";
    private static final String FIELD_SPAN_ID = "spanId";
    private static final String FIELD_PAYLOAD = "payload";

    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CLIENT_ERROR_MIN = 400;
    private static final int HTTP_SERVER_ERROR_MIN = 500;

    private final JsonCodec codec;

    public ErrorMapper(JsonCodec codec) {
        this.codec = codec;
    }

    public ErliApiException toException(int httpStatus, String rawBody) {
        ErliErrorDetails details = parse(httpStatus, rawBody);
        if (httpStatus == HTTP_UNAUTHORIZED || httpStatus == HTTP_FORBIDDEN) {
            return new ErliAuthException(details);
        }
        if (httpStatus == HTTP_NOT_FOUND) {
            return new ErliNotFoundException(details);
        }
        if (httpStatus >= HTTP_SERVER_ERROR_MIN) {
            return new ErliServerException(details);
        }
        if (httpStatus >= HTTP_CLIENT_ERROR_MIN) {
            return new ErliValidationException(details);
        }
        // Any other non-2xx (e.g. an unexpected 3xx that reached here) is treated as a server fault.
        return new ErliServerException(details);
    }

    private ErliErrorDetails parse(int httpStatus, String rawBody) {
        JsonNode body = codec.readTreeLenient(rawBody);
        return new ErliErrorDetails(
                httpStatus,
                intOrNull(body, FIELD_HTTP_CODE),
                textOrNull(body, FIELD_FAILURE_TYPE),
                textOrNull(body, FIELD_NAME),
                textOrNull(body, FIELD_MESSAGE),
                textOrNull(body, FIELD_POLISH_MESSAGE),
                textOrNull(body, FIELD_TRACE_ID),
                textOrNull(body, FIELD_SPAN_ID),
                payloadOrNull(body),
                rawBody);
    }

    private static String textOrNull(JsonNode body, String field) {
        if (body == null) {
            return null;
        }
        JsonNode value = body.get(field);
        return value != null && value.isValueNode() ? value.asText() : null;
    }

    /**
     * The {@code payload} field, preserved whatever its JSON shape (CORE-8). The API sends it as a
     * scalar on some errors ({@code "payload":"Invalid API key"}) but as an object carrying per-field
     * validation detail on others ({@code "payload":{"details":{...}}}). A plain text extraction would
     * drop the object form; this keeps it as its compact JSON string so no diagnostic detail is lost.
     */
    private static String payloadOrNull(JsonNode body) {
        if (body == null) {
            return null;
        }
        JsonNode value = body.get(FIELD_PAYLOAD);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.isValueNode() ? value.asText() : value.toString();
    }

    private static Integer intOrNull(JsonNode body, String field) {
        if (body == null) {
            return null;
        }
        JsonNode value = body.get(field);
        return value != null && value.isNumber() ? value.asInt() : null;
    }
}
