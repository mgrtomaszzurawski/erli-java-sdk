package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * Structured, log-safe view of an Erli error response.
 *
 * <p>The live API returns a body <em>richer than the published {@code Error} schema</em>: a 401 was
 * observed as {@code {failureType:"security", message, httpCode:401, polishMessage, payload, spanId}}
 * with no {@code traceId}. Every field except {@link #httpStatus()} is therefore nullable — the
 * mapper fills what it finds and preserves the raw body for diagnostics. See
 * {@code context/repo-docs/KNOWN-SERVER-BEHAVIORS.md}.
 *
 * @param httpStatus     the HTTP status actually received on the wire (always known)
 * @param bodyHttpCode   the {@code httpCode} echoed inside the body, or {@code null} (may equal httpStatus)
 * @param failureType    the {@code failureType} discriminator, or {@code null} (spec lists
 *                       {@code failure|conflict}; the live API also emits {@code security}, so this
 *                       is treated as a free string)
 * @param name           the {@code name} field, or {@code null}
 * @param message        the {@code message} field, or {@code null}
 * @param polishMessage  the operator-facing Polish message, or {@code null}
 * @param traceId        the distributed-trace id, or {@code null} (absent on some responses)
 * @param spanId         the span id, or {@code null}
 * @param payload        the extra {@code payload} field observed live, or {@code null}
 * @param rawBody        the response body verbatim, for diagnostics when parsing was partial
 */
public record ErliErrorDetails(
        int httpStatus,
        Integer bodyHttpCode,
        String failureType,
        String name,
        String message,
        String polishMessage,
        String traceId,
        String spanId,
        String payload,
        String rawBody) {

    /**
     * Log-safe rendering. The record's default {@code toString} would print {@link #rawBody()} and
     * {@link #payload()} verbatim — and on 4xx responses from write endpoints those echo the request,
     * which can carry buyer PII (e-mail, address, courier instructions). This override omits both; the
     * accessors still expose them for deliberate, non-logging use. (CORE-11.)
     */
    @Override
    public String toString() {
        return "ErliErrorDetails[httpStatus=" + httpStatus
                + ", failureType=" + failureType
                + ", name=" + name
                + ", traceId=" + traceId
                + ", spanId=" + spanId
                + ", rawBody=<redacted>]";
    }
}
