package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * The request was rejected as invalid (Erli {@code 1200}, typically HTTP 400/409/422). The
 * remediation is to fix the request; retrying the same payload will not help.
 */
public final class ErliValidationException extends ErliApiException {

    private static final long serialVersionUID = 1L;

    public ErliValidationException(ErliErrorDetails details) {
        super(details);
    }
}
