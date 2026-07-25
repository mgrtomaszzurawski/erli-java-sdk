package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * Root of every exception thrown by the Erli SDK. Catch this to handle all SDK failures uniformly;
 * catch a subtype to branch on remediation (auth vs. validation vs. server vs. transport).
 */
public abstract class ErliException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected ErliException(String message) {
        super(message);
    }

    protected ErliException(String message, Throwable cause) {
        super(message, cause);
    }
}
