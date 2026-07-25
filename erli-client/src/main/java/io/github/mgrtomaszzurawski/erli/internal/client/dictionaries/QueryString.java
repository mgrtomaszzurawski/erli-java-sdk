package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Builds a percent-encoded {@code ?a=1&b=2} suffix for the dictionary endpoints that take optional
 * query filters. Parameters whose value is absent are dropped, so an empty filter yields an empty
 * string and the request path is unchanged.
 *
 * <p>Bucket-local on purpose: {@code HttpRuntime} has no query-parameter support yet (BACKLOG
 * CORE-1). When core grows a shared, typed query carrier this class should be deleted in favour of
 * it — it is duplication waiting to be absorbed, not a second transport.
 *
 * <p>Internal: never exported.
 */
final class QueryString {

    private static final String QUERY_PREFIX = "?";
    private static final String PARAMETER_SEPARATOR = "&";
    private static final String KEY_VALUE_SEPARATOR = "=";

    private final List<String> parameters = new ArrayList<>();

    private QueryString() {
    }

    static QueryString builder() {
        return new QueryString();
    }

    /** Append {@code name=value} when the value is present; otherwise do nothing. */
    QueryString add(String name, Optional<?> value) {
        value.ifPresent(present -> parameters.add(encode(name) + KEY_VALUE_SEPARATOR + encode(present.toString())));
        return this;
    }

    /**
     * The encoded query suffix including its leading {@code ?}, or an empty string when no parameter
     * was set.
     */
    String build() {
        if (parameters.isEmpty()) {
            return "";
        }
        StringJoiner joined = new StringJoiner(PARAMETER_SEPARATOR, QUERY_PREFIX, "");
        parameters.forEach(joined::add);
        return joined.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
