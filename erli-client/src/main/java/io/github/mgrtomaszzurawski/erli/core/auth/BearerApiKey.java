package io.github.mgrtomaszzurawski.erli.core.auth;

import java.util.Objects;

/**
 * {@link ApiKey} backed by a raw Bearer token. Immutable and value-equal on the token, but its
 * {@link #toString()} and {@link #redacted()} deliberately never expose the token so it cannot leak
 * into logs. The only path to the secret is {@link #authorizationHeaderValue()}, called by transport.
 */
public final class BearerApiKey implements ApiKey {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REDACTED_RENDERING = "BearerApiKey[REDACTED]";

    private final String token;

    BearerApiKey(String rawKey) {
        Objects.requireNonNull(rawKey, "rawKey");
        String trimmed = rawKey.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("API key must not be blank");
        }
        this.token = trimmed;
    }

    @Override
    public String authorizationHeaderValue() {
        return BEARER_PREFIX + token;
    }

    @Override
    public String redacted() {
        return REDACTED_RENDERING;
    }

    @Override
    public String toString() {
        return REDACTED_RENDERING;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BearerApiKey that && token.equals(that.token);
    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }
}
