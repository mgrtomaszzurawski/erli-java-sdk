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
 * <p><strong>A subscription this SDK registers must use {@code https}.</strong> Erli sends the
 * {@link #accessToken()} and, for the three {@code ORDER_*} kinds, the buyer's personal data to this
 * endpoint; over {@code http} both would cross the network in the clear. A URL carrying credentials in
 * its userinfo ({@code https://user:pass@…}) is rejected for the same reason. The rule is applied when
 * a subscription is <em>built or saved</em>, not when one is read back — see the constructor.
 *
 * <p>{@link #toString()} redacts {@link #accessToken()} and prints the URL without its query string,
 * because a webhook URL commonly carries the shared secret as a query parameter.
 *
 * @param kind        which subscription this is
 * @param url         the {@code https} shop endpoint Erli calls (at most {@value #MAX_URL_LENGTH} characters)
 * @param accessToken the token Erli sends back to the shop, if configured
 */
public record Hook(HookKind kind, URI url, Optional<String> accessToken) {

    private static final String FIELD_ACCESS_TOKEN = "accessToken";

    /** The API's limit on the hook URL. */
    public static final int MAX_URL_LENGTH = 2000;
    /** The API's limit on the access token. */
    public static final int MAX_ACCESS_TOKEN_LENGTH = 400;

    private static final String REQUIRED_SCHEME = "https";
    private static final String REDACTED = "***";

    /**
     * Accepts whatever the API can hold. The {@code https} rule is <em>not</em> applied here on
     * purpose: this constructor is also how a subscription read back from the API becomes a record, and
     * a shop may already have registered an {@code http} endpoint through the panel or another client.
     * Refusing to represent it would make {@link HooksAccess#list()} fail wholesale — precisely when the
     * shop needs to see the insecure subscription in order to replace it. Construction that
     * <em>originates</em> in consumer code goes through {@link #of} and is checked there.
     */
    public Hook {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(accessToken, FIELD_ACCESS_TOKEN);
        requireAtMost(url.toString(), MAX_URL_LENGTH, "url");
        accessToken.ifPresent(token -> requireAtMost(token, MAX_ACCESS_TOKEN_LENGTH, FIELD_ACCESS_TOKEN));
    }

    /** A subscription without an access token. The URL must be a plain {@code https} endpoint. */
    public static Hook of(HookKind kind, URI url) {
        return validated(new Hook(kind, url, Optional.empty()));
    }

    /** A subscription whose calls carry an access token. The URL must be a plain {@code https} endpoint. */
    public static Hook of(HookKind kind, URI url, String accessToken) {
        return validated(new Hook(kind, url, Optional.of(Objects.requireNonNull(accessToken, FIELD_ACCESS_TOKEN))));
    }

    /**
     * Check that this subscription is safe to register. Applied by {@link #of} and again by
     * {@code save}, so an insecure endpoint cannot reach the API however the record was built.
     *
     * @return this subscription
     * @throws IllegalArgumentException if the endpoint is not a plain {@code https} URL
     */
    public Hook requireRegisterable() {
        return validated(this);
    }

    private static Hook validated(Hook hook) {
        requireSecureEndpoint(hook.url());
        return hook;
    }

    @Override
    public String toString() {
        return "Hook[kind=" + kind
                + ", url=" + describeUrl()
                + ", accessToken=" + (accessToken.isPresent() ? REDACTED : "absent")
                + "]";
    }

    /**
     * The endpoint without anything that commonly carries a secret: userinfo, query and fragment. The
     * port is kept — a redacted line naming the wrong endpoint is worse than one naming none.
     */
    private String describeUrl() {
        String port = url.getPort() < 0 ? "" : ":" + url.getPort();
        // getRawPath, not getPath: the decoded form turns a %0A in the URL into a real newline, which
        // would let a crafted endpoint forge extra log lines. Keep it raw.
        String path = url.getRawPath() == null ? "" : url.getRawPath();
        String query = url.getRawQuery() == null ? "" : "?" + REDACTED;
        return url.getScheme() + "://" + url.getHost() + port + path + query;
    }

    private static void requireSecureEndpoint(URI url) {
        if (!REQUIRED_SCHEME.equalsIgnoreCase(url.getScheme())) {
            throw new IllegalArgumentException(
                    "url scheme must be " + REQUIRED_SCHEME + ", but was: " + url.getScheme());
        }
        // These two checks are jointly load-bearing, so neither is redundant: with a registry-based
        // authority (https://user:pass@host_name/...) java.net.URI reports no userinfo at all and the
        // credentials hide in the raw authority — that shape is caught by the null host, not below.
        if (url.getHost() == null) {
            throw new IllegalArgumentException("url must be absolute and name a host, but was: " + url);
        }
        if (url.getRawUserInfo() != null) {
            throw new IllegalArgumentException("url must not carry credentials in its userinfo component");
        }
    }

    private static void requireAtMost(String value, int maxLength, String field) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
    }
}
