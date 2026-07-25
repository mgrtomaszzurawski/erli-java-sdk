package io.github.mgrtomaszzurawski.erli.internal.client.finance;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a percent-encoded query string to append to an {@code ApiPaths} constant.
 *
 * <p><strong>Interim, bucket-local.</strong> {@code HttpRuntime} froze at M1 with
 * {@code get(path, type)} / {@code post(path, body, type)} and no query-parameter support, and every
 * bucket needs it (see {@code BACKLOG.md} → CORE-1, raised by agent-4). Rather than edit core — which
 * the fan-out plan reserves for the core owner — the Finance areas append an encoded suffix to their
 * path constant. Fold this into {@code HttpRuntime.baseRequest} when CORE-1 lands and delete it.
 *
 * <p>Values are encoded, so a caller-supplied value cannot inject a separator or alter the path.
 * Null values are skipped, matching the API's rule that an absent optional is not the same as an
 * explicitly-empty one. Internal: never exported.
 */
public final class QueryString {

    private static final String QUERY_START = "?";
    private static final String PARAMETER_SEPARATOR = "&";
    private static final String KEY_VALUE_SEPARATOR = "=";

    private final Map<String, String> parameters = new LinkedHashMap<>();

    /** Add a parameter unless its value is {@code null}. */
    public QueryString add(String name, String value) {
        if (value != null) {
            parameters.put(name, value);
        }
        return this;
    }

    /**
     * Render as {@code "?a=1&b=2"}, or the empty string when no parameter was set, so the result can
     * always be concatenated onto a path constant.
     */
    public String render() {
        if (parameters.isEmpty()) {
            return "";
        }
        StringBuilder rendered = new StringBuilder(QUERY_START);
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (rendered.length() > QUERY_START.length()) {
                rendered.append(PARAMETER_SEPARATOR);
            }
            rendered.append(encode(parameter.getKey()))
                    .append(KEY_VALUE_SEPARATOR)
                    .append(encode(parameter.getValue()));
        }
        return rendered.toString();
    }

    /**
     * Substitute a {@code {name}} placeholder in a path template with an encoded value, so an id
     * containing {@code /}, {@code ?} or {@code #} cannot alter the request path.
     */
    public static String pathParameter(String pathTemplate, String placeholder, String value) {
        // URLEncoder targets form encoding, where a space is "+". That is correct in a query but
        // wrong in a path segment, where it must be %20.
        String encodedSegment = encode(value).replace("+", "%20");
        return pathTemplate.replace("{" + placeholder + "}", encodedSegment);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
