package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * A request could not complete at the transport level — an I/O failure, a timeout, an interruption,
 * or a body that could not be decoded. No usable HTTP response was obtained, so there is no
 * {@link ErliErrorDetails}; inspect {@link #getCause()} for the underlying failure.
 */
public final class ErliTransportException extends ErliException {

    private static final long serialVersionUID = 1L;

    public ErliTransportException(String message, Throwable cause) {
        super(message, cause);
    }

    public ErliTransportException(String message) {
        super(message);
    }
}
