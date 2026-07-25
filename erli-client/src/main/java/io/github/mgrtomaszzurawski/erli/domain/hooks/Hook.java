package io.github.mgrtomaszzurawski.erli.domain.hooks;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * A webhook subscription: the endpoint Erli calls for one {@link HookKind}, plus the optional token
 * the shop wants echoed back so it can authorise the call.
 *
 * <p>The same record is both what {@code GET /hooks} returns and what {@code PUT /hooks/{hookName}}
 * accepts — the API's read and write shapes are identical.
 *
 * <p>{@link #toString()} redacts {@link #accessToken()}: it is a credential of the shop's own system
 * and must not reach a log.
 *
 * @param kind        which subscription this is
 * @param url         the shop endpoint Erli calls (at most {@value #MAX_URL_LENGTH} characters)
 * @param accessToken the token Erli sends back to the shop, if configured
 */
public record Hook(HookKind kind, URI url, Optional<String> accessToken) {

    /** The API's limit on the hook URL. */
    public static final int MAX_URL_LENGTH = 2000;
    /** The API's limit on the access token. */
    public static final int MAX_ACCESS_TOKEN_LENGTH = 400;

    private static final String REDACTED_TOKEN = "***";

    public Hook {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(accessToken, "accessToken");
        requireAtMost(url.toString(), MAX_URL_LENGTH, "url");
        accessToken.ifPresent(token -> requireAtMost(token, MAX_ACCESS_TOKEN_LENGTH, "accessToken"));
    }

    /** A subscription without an access token. */
    public static Hook of(HookKind kind, URI url) {
        return new Hook(kind, url, Optional.empty());
    }

    /** A subscription whose calls carry an access token. */
    public static Hook of(HookKind kind, URI url, String accessToken) {
        return new Hook(kind, url, Optional.of(accessToken));
    }

    @Override
    public String toString() {
        return "Hook[kind=" + kind
                + ", url=" + url
                + ", accessToken=" + (accessToken.isPresent() ? REDACTED_TOKEN : "absent")
                + "]";
    }

    private static void requireAtMost(String value, int maxLength, String field) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
    }
}
