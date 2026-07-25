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
 * <p><strong>The URL must be {@code https}.</strong> Erli sends the {@link #accessToken()} and, for the
 * three {@code ORDER_*} kinds, the buyer's personal data to this endpoint; over {@code http} both would
 * cross the network in the clear. A URL carrying credentials in its userinfo ({@code https://user:pass@…})
 * is rejected for the same reason.
 *
 * <p>{@link #toString()} redacts {@link #accessToken()} and prints the URL without its query string,
 * because a webhook URL commonly carries the shared secret as a query parameter.
 *
 * @param kind        which subscription this is
 * @param url         the {@code https} shop endpoint Erli calls (at most {@value #MAX_URL_LENGTH} characters)
 * @param accessToken the token Erli sends back to the shop, if configured
 */
public record Hook(HookKind kind, URI url, Optional<String> accessToken) {

    /** The API's limit on the hook URL. */
    public static final int MAX_URL_LENGTH = 2000;
    /** The API's limit on the access token. */
    public static final int MAX_ACCESS_TOKEN_LENGTH = 400;

    private static final String REQUIRED_SCHEME = "https";
    private static final String REDACTED = "***";

    public Hook {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(accessToken, "accessToken");
        requireAtMost(url.toString(), MAX_URL_LENGTH, "url");
        requireSecureEndpoint(url);
        accessToken.ifPresent(token -> requireAtMost(token, MAX_ACCESS_TOKEN_LENGTH, "accessToken"));
    }

    /** A subscription without an access token. */
    public static Hook of(HookKind kind, URI url) {
        return new Hook(kind, url, Optional.empty());
    }

    /** A subscription whose calls carry an access token. */
    public static Hook of(HookKind kind, URI url, String accessToken) {
        return new Hook(kind, url, Optional.of(Objects.requireNonNull(accessToken, "accessToken")));
    }

    @Override
    public String toString() {
        return "Hook[kind=" + kind
                + ", url=" + describeUrl()
                + ", accessToken=" + (accessToken.isPresent() ? REDACTED : "absent")
                + "]";
    }

    /** The endpoint without anything that commonly carries a secret: userinfo, query and fragment. */
    private String describeUrl() {
        String path = url.getRawPath() == null ? "" : url.getRawPath();
        String suffix = url.getRawQuery() == null ? "" : "?" + REDACTED;
        return url.getScheme() + "://" + url.getHost() + path + suffix;
    }

    private static void requireSecureEndpoint(URI url) {
        if (!REQUIRED_SCHEME.equalsIgnoreCase(url.getScheme()) || url.getHost() == null) {
            throw new IllegalArgumentException(
                    "url must be an absolute " + REQUIRED_SCHEME + " URL with a host, but was: " + url.getScheme());
        }
        if (url.getRawUserInfo() != null) {
            throw new IllegalArgumentException(
                    "url must not carry credentials in its userinfo component");
        }
    }

    private static void requireAtMost(String value, int maxLength, String field) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
    }
}
