package io.github.mgrtomaszzurawski.erli.core.auth;

import io.github.mgrtomaszzurawski.erli.core.error.ErliConfigurationException;

/**
 * A single static Erli API key, presented to the API as an HTTP Bearer token.
 *
 * <p>Erli authenticates with one key issued from the shop panel — there is no OAuth2, no refresh and
 * no rotation (see {@code ADR-003}). The credential type is sealed so the shape stays closed, and the
 * raw token is never revealed by {@link #toString()} or {@link #redacted()}; only the transport reads
 * {@link #authorizationHeaderValue()}.
 */
public sealed interface ApiKey permits BearerApiKey {

    /** Environment variable that holds the raw key (mirrors {@code /workspace/shared/secrets}). */
    String API_KEY_ENV_VAR = "ERLI_API_KEY";

    /**
     * Wrap a raw key string. The value is trimmed; blank input is rejected.
     *
     * @param rawKey the raw Bearer token issued by the Erli shop panel
     * @return a Bearer credential
     */
    static ApiKey of(String rawKey) {
        return new BearerApiKey(rawKey);
    }

    /**
     * Read the key from the {@value #API_KEY_ENV_VAR} environment variable.
     *
     * @return a Bearer credential built from the environment
     * @throws ErliConfigurationException if the variable is unset or blank
     */
    static ApiKey fromEnvironment() {
        String value = System.getenv(API_KEY_ENV_VAR);
        if (value == null || value.isBlank()) {
            throw new ErliConfigurationException(
                    "Environment variable " + API_KEY_ENV_VAR + " is not set or is blank");
        }
        return of(value);
    }

    /** The full {@code Authorization} header value, i.e. {@code "Bearer <key>"}. */
    String authorizationHeaderValue();

    /** A log-safe rendering of the credential that never contains the token itself. */
    String redacted();
}
