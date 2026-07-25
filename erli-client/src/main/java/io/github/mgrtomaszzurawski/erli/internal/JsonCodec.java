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
 * Thin Jackson wrapper for the SDK's JSON boundary. Internal: never exported to consumers.
 *
 * <p>The configuration is the contract every layer above depends on:
 * <ul>
 *   <li><strong>Unknown properties are ignored</strong>, so the SDK stays forward-compatible with the
 *       API's richer-than-spec payloads (see {@code KNOWN-SERVER-BEHAVIORS.md}).</li>
 *   <li><strong>{@code java.time} types are supported</strong> — every OpenAPI {@code date-time}
 *       property becomes an {@link java.time.OffsetDateTime} in the generated Layer-1 models.</li>
 *   <li><strong>Dates travel as ISO-8601 strings</strong>, never as epoch numbers; the spec types every
 *       timestamp as {@code string/date-time}, in requests as well as responses.</li>
 *   <li><strong>The offset the API sent is preserved</strong> rather than normalized to UTC.</li>
 * </ul>
 */
public final class JsonCodec {

    private final ObjectMapper mapper;

    public JsonCodec() {
        this.mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Layer 1 maps every OpenAPI `date-time` property to java.time.OffsetDateTime, which a
                // bare ObjectMapper refuses to handle. Erli states timestamps as ISO-8601 strings, so
                // dates must also serialize as strings rather than epoch numbers.
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Keep the offset the API actually sent. Jackson otherwise rewrites every timestamp to
                // UTC, which silently changes what OffsetDateTime.getOffset() reports; the instant is
                // the same, but the SDK would be handing back a value the server never stated.
                .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                // Layer 1 maps every `nullable: true` property to JsonNullable<T> (100 of them across
                // the spec, mostly the product schemas). Without this module such a property cannot be
                // decoded at all when it carries a value, and throws when it carries an explicit null.
                .registerModule(new JsonNullableModule())
                // An unset optional field must be omitted from a write body, not sent as an explicit
                // null: the API's request schemas are additionalProperties:false and treat a null as a
                // value to store. This also lets JsonNullable's "undefined" state stay off the wire.
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                // Unknown enum values decode to null instead of throwing (CORE-12). A Layer-1 enum is a
                // snapshot of the vendored spec; when the API grows one (a new PayU method, a new
                // carrier), a strict decoder would fail the WHOLE response over a single field. With
                // this, decoding survives and the field is null; the domain layer then decides — a
                // required-field null-check keeps closed enums fail-loud, and open/growing enums may
                // instead map null to an UNRECOGNIZED sentinel. See KNOWN-SERVER-BEHAVIORS.md "Enum
                // handling". Note: an unknown value on an OPTIONAL enum field is now silently absent.
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
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

    /**
     * Bind an already-parsed JSON subtree to {@code type}.
     *
     * <p>Needed wherever the SDK must choose the target class itself instead of letting Jackson choose.
     * The generated {@code anyOf} wrappers try their branches in declaration order and accept the first
     * that does not throw; with unknown properties ignored (see the class javadoc) the first branch
     * always wins, so a payload whose real shape is a later branch binds to the wrong class and loses
     * its fields silently. A caller that knows the discriminator — for inbox messages the sibling
     * {@code type} field — reads the tree and binds the correct branch through this method.
     */
    public <T> T convert(JsonNode node, Class<T> type) {
        try {
            return mapper.treeToValue(node, type);
        } catch (JsonProcessingException failure) {
            throw new ErliTransportException("Failed to bind JSON to " + type.getSimpleName(), failure);
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
