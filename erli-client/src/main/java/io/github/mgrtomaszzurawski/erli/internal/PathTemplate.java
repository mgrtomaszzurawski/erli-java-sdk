package io.github.mgrtomaszzurawski.erli.internal;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Expands a path template with {@code {name}} placeholders, URL-encoding each value as a single path
 * segment. Owned by core so every by-id endpoint ({@code /products/{externalId}},
 * {@code /orders/{id}}, {@code /hooks/{hookName}}, …) shares one safe implementation instead of naive
 * {@code replace("{id}", value)} — which lets an id containing {@code /}, {@code ?}, {@code #} or a
 * {@code ..} segment re-route the request to a different resource (a real injection risk). Segments
 * are percent-encoded (space as {@code %20}, not {@code +}); {@code .} and {@code ..} are rejected.
 * Internal: never exported.
 */
public final class PathTemplate {

    private static final String ENCODED_SPACE = "%20";
    private static final String PLUS = "+";
    private static final char OPEN_PLACEHOLDER = '{';

    private PathTemplate() {
    }

    /**
     * @param template a path with {@code {name}} placeholders, e.g. {@code "/products/{externalId}"}
     * @param values   the value for each placeholder name
     * @return the path with every placeholder replaced by its encoded value
     * @throws IllegalArgumentException if a value is missing, blank, a {@code .}/{@code ..} segment,
     *                                  or if a placeholder is left unresolved
     */
    public static String expand(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = OPEN_PLACEHOLDER + entry.getKey() + "}";
            if (!result.contains(placeholder)) {
                throw new IllegalArgumentException("Template has no placeholder " + placeholder + ": " + template);
            }
            result = result.replace(placeholder, encodeSegment(entry.getKey(), entry.getValue()));
        }
        if (result.indexOf(OPEN_PLACEHOLDER) >= 0) {
            throw new IllegalArgumentException("Unresolved path parameter in " + result);
        }
        return result;
    }

    private static String encodeSegment(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Path parameter '" + name + "' must not be null or blank");
        }
        String trimmed = value.trim();
        if (".".equals(trimmed) || "..".equals(trimmed)) {
            throw new IllegalArgumentException("Path parameter '" + name + "' must not be '" + trimmed + "'");
        }
        return URLEncoder.encode(trimmed, StandardCharsets.UTF_8).replace(PLUS, ENCODED_SPACE);
    }
}
