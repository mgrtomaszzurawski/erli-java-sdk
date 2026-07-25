package io.github.mgrtomaszzurawski.erli.internal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

/**
 * Thin Jackson wrapper for the SDK's JSON boundary. Configured to <strong>ignore unknown
 * properties</strong> so the SDK stays forward-compatible with the API's richer-than-spec payloads
 * (see {@code KNOWN-SERVER-BEHAVIORS.md}). Internal: never exported to consumers.
 */
public final class JsonCodec {

    private final ObjectMapper mapper;

    public JsonCodec() {
        this.mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /** Deserialize a response body into {@code type}, wrapping any failure as a transport error. */
    public <T> T read(String body, Class<T> type) {
        try {
            return mapper.readValue(body, type);
        } catch (Exception failure) {
            throw new ErliTransportException("Failed to decode response body as " + type.getSimpleName(), failure);
        }
    }

    /** Best-effort tree parse for error bodies; returns {@code null} if the body is not JSON. */
    public JsonNode readTreeLenient(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Serialize a request body, wrapping any failure as a transport error. */
    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception failure) {
            throw new ErliTransportException("Failed to encode request body", failure);
        }
    }
}
