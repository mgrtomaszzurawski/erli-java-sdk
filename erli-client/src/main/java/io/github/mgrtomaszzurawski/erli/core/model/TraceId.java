package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * A distributed-trace identifier echoed by the API for correlation. Present on most responses; error
 * bodies also carry a {@code spanId}. Kept as a typed value so it is not confused with other strings.
 *
 * @param value the non-blank trace id
 */
public record TraceId(String value) {

    public TraceId {
        value = Identifiers.requireText(value, "TraceId");
    }

    public static TraceId of(String value) {
        return new TraceId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
