package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Percent-encodes a value being substituted into a templated path, so a price-list name containing a
 * slash or a space cannot change which endpoint is called.
 *
 * <p>{@link URLEncoder} targets form encoding, where a space becomes {@code +}; inside a path segment
 * a space must be {@code %20}, so that one substitution is corrected here.
 *
 * <p>Bucket-local for the same reason as {@link QueryString}: core owns no path helper yet
 * (BACKLOG CORE-1). Internal: never exported.
 */
final class PathSegment {

    private static final String FORM_ENCODED_SPACE = "+";
    private static final String PATH_ENCODED_SPACE = "%20";

    private PathSegment() {
    }

    /** Encode {@code value} for safe use as a single path segment. */
    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace(FORM_ENCODED_SPACE, PATH_ENCODED_SPACE);
    }
}
