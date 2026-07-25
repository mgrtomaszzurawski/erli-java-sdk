package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * The API returned a non-success HTTP response. Subtypes group failures by the remediation the caller
 * should take, aligned with Erli's documented code families: {@code 1100} server, {@code 1200}
 * validation, {@code 1300} auth, {@code 1400} not-found. The structured {@link ErliErrorDetails} is
 * always attached.
 */
public abstract sealed class ErliApiException extends ErliException
        permits ErliServerException, ErliValidationException, ErliAuthException, ErliNotFoundException {

    private static final long serialVersionUID = 1L;

    private final transient ErliErrorDetails details;

    protected ErliApiException(ErliErrorDetails details) {
        super(buildMessage(details));
        this.details = details;
    }

    /** The structured error payload: HTTP status, Erli fields, traceId/spanId, and the raw body. */
    public ErliErrorDetails details() {
        return details;
    }

    private static String buildMessage(ErliErrorDetails details) {
        StringBuilder builder = new StringBuilder();
        builder.append("HTTP ").append(details.httpStatus());
        if (details.failureType() != null) {
            builder.append(" [").append(details.failureType()).append(']');
        }
        if (details.message() != null) {
            builder.append(": ").append(details.message());
        }
        if (details.traceId() != null) {
            builder.append(" (traceId=").append(details.traceId()).append(')');
        } else if (details.spanId() != null) {
            builder.append(" (spanId=").append(details.spanId()).append(')');
        }
        return builder.toString();
    }
}
