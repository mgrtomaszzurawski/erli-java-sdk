package io.github.mgrtomaszzurawski.erli.internal.client.finance;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Fills a {@code {name}} placeholder in an {@code ApiPaths} template with an encoded value.
 *
 * <p><strong>Interim, bucket-local.</strong> CORE-1 gave the transport {@code QueryParameters}, but a
 * shared path-template helper is still open on the core owner's list (see {@code BACKLOG.md}), and
 * every bucket with an {@code {id}} path currently repeats {@code template.replace("{id}", value)} —
 * which does no encoding. Delete this and switch when core lands one. Internal: never exported.
 */
public final class PathParameters {

    private static final String PLACEHOLDER_PREFIX = "{";
    private static final String PLACEHOLDER_SUFFIX = "}";
    private static final String ENCODED_SPACE_FORM = "+";
    private static final String ENCODED_SPACE_PATH = "%20";

    private PathParameters() {
    }

    /**
     * Substitute one placeholder, encoding the value so an id containing {@code /}, {@code ?} or
     * {@code #} cannot escape its path segment and alter the request line.
     *
     * @param pathTemplate the path with a {@code {name}} placeholder
     * @param name         the placeholder name, without braces
     * @param value        the value to substitute
     * @return the filled path
     */
    public static String fill(String pathTemplate, String name, String value) {
        // URLEncoder targets form encoding, where a space is "+". That is correct in a query but
        // wrong in a path segment, where it must be %20.
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace(ENCODED_SPACE_FORM, ENCODED_SPACE_PATH);
        return pathTemplate.replace(PLACEHOLDER_PREFIX + name + PLACEHOLDER_SUFFIX, encoded);
    }
}
