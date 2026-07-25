package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * Authentication or authorization failed (Erli {@code 1300}, HTTP 401/403; the live API tags these
 * {@code failureType:"security"}). The remediation is to fix the credential or permissions; retrying
 * the same key will not help.
 */
public final class ErliAuthException extends ErliApiException {

    private static final long serialVersionUID = 1L;

    public ErliAuthException(ErliErrorDetails details) {
        super(details);
    }
}
