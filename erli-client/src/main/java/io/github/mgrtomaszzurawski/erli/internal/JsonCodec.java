package io.github.mgrtomaszzurawski.erli.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

import java.util.List;

/**
 * Thin Jackson wrapper for the SDK's JSON boundary. Configured to <strong>ignore unknown
 * properties</strong> so the SDK stays forward-compatible with the API's richer-than-spec payloads
 * (see {@code KNOWN-SERVER-BEHAVIORS.md}). Internal: never exported to consumers.
 *
 * <p>Three further settings are load-bearing for every domain bucket:
 * <ul>
 *   <li>the JSR-310 module, because the generated models expose {@code date-time} properties as
 *       {@link java.time.OffsetDateTime} — without it decoding any dated payload fails outright;</li>
 *   <li>ISO-8601 (not numeric) date output, matching what the API sends and accepts;</li>
 *   <li>{@link JsonInclude.Include#NON_NULL} serialization, so an unset optional property is
 *       <em>omitted</em> rather than written as an explicit {@code null}. This matters on the
 *       {@code PATCH} endpoints, where an explicit {@code null} is a request to clear the field.</li>
 * </ul>
 */
public final class JsonCodec {

    private final ObjectMapper mapper;

    public JsonCodec() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Jackson otherwise rewrites every parsed offset to UTC. The instant survives, but an
                // OffsetDateTime the SDK hands a consumer would silently lose the offset the server
                // actually sent, which is a detail a public java.time type is expected to preserve.
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /** Deserialize a response body into {@code type}, wrapping any failure as a transport error. */
    public <T> T read(String body, Class<T> type) {
        try {
            return mapper.readValue(body, type);
        } catch (JsonProcessingException failure) {
            throw new ErliTransportException("Failed to decode response body as " + type.getSimpleName(), failure);
        }
    }

    /**
     * Deserialize a JSON array body into a {@code List<T>}. Erli's dictionary endpoints return bare
     * arrays (no pagination envelope), so this is the list counterpart to {@link #read}.
     */
    public <T> List<T> readList(String body, Class<T> elementType) {
        try {
            CollectionType listType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return mapper.readValue(body, listType);
        } catch (JsonProcessingException failure) {
            throw new ErliTransportException(
                    "Failed to decode response body as List<" + elementType.getSimpleName() + ">", failure);
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
        } catch (JsonProcessingException failure) {
            throw new ErliTransportException("Failed to encode request body", failure);
        }
    }
}
