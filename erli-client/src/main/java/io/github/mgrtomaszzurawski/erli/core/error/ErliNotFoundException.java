package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * The addressed resource does not exist (Erli {@code 1400}, HTTP 404). The remediation is to correct
 * the identifier; retrying the same path will not help.
 */
public final class ErliNotFoundException extends ErliApiException {

    private static final long serialVersionUID = 1L;

    public ErliNotFoundException(ErliErrorDetails details) {
        super(details);
    }
}
