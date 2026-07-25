package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * The server failed to process a well-formed request (Erli {@code 1100}, HTTP 5xx). The remediation
 * is to retry later; these responses are eligible for the {@code RetryPolicy}.
 */
public final class ErliServerException extends ErliApiException {

    private static final long serialVersionUID = 1L;

    public ErliServerException(ErliErrorDetails details) {
        super(details);
    }
}
