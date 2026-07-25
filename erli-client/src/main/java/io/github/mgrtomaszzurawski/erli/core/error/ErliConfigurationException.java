package io.github.mgrtomaszzurawski.erli.core.error;

/**
 * The client was misconfigured before any request could be made — e.g. a missing API key or base
 * URL. This is a caller-side setup error, distinct from an API response or a transport failure.
 */
public final class ErliConfigurationException extends ErliException {

    private static final long serialVersionUID = 1L;

    public ErliConfigurationException(String message) {
        super(message);
    }
}
