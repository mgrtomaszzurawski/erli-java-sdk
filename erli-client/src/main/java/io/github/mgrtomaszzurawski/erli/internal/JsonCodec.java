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
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.util.List;

/**
 * Thin Jackson wrapper for the SDK's JSON boundary. Configured to <strong>ignore unknown
 * properties</strong> so the SDK stays forward-compatible with the API's richer-than-spec payloads
 * (see {@code KNOWN-SERVER-BEHAVIORS.md}). Internal: never exported to consumers.
 *
 * <p>The reading side registers the two datatype modules the generated Layer-1 models need:
 * {@code JavaTimeModule} for every {@code format: date-time} property and {@code JsonNullableModule}
 * for the generator's {@code JsonNullable} fields. The writing side omits {@code null}s, because the
 * Erli API rejects an explicitly-null optional rather than treating it as absent (observed on
 * {@code /billing/company/entries}, {@code /commissions/_estimate} and
 * {@code /payments/operations/_search}), and because on a {@code PATCH} an explicit null reads as
 * "clear this field". See {@code KNOWN-SERVER-BEHAVIORS.md}.
 */
public final class JsonCodec {

    private final ObjectMapper mapper;

    public JsonCodec() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new JsonNullableModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Keep the offset the API sent (Erli reports Polish local time, e.g. +02:00) instead
                // of silently rewriting it to UTC. Same instant either way, but a settlement or
                // payout timestamp should read back as the marketplace stated it.
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                // ISO-8601 strings, not epoch numbers — the API speaks RFC 3339 timestamps.
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
