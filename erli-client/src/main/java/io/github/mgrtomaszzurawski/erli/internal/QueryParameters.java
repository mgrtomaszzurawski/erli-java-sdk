package io.github.mgrtomaszzurawski.erli.internal;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An ordered, URL-encoded set of query parameters for a request. Supports the forms the Erli API uses:
 * a single value, a comma-joined list ({@code parcelIds=1,2,3}), repeated keys ({@code id=1&id=2}),
 * and bracketed repeated keys ({@code id[]=1&id[]=2}). Null values are skipped so callers can pass
 * optional filters directly. Domain filter objects build one of these in their bucket's internal
 * client code — this type is transport-internal and never exported. Internal.
 */
public final class QueryParameters {

    private static final QueryParameters EMPTY = new QueryParameters(List.of());
    private static final String BRACKET_SUFFIX = "[]";
    private static final char FIRST_SEPARATOR = '?';
    private static final char NEXT_SEPARATOR = '&';
    private static final String CSV_DELIMITER = ",";
    private static final String ENCODED_COMMA = "%2C";

    private final List<Map.Entry<String, String>> pairs;

    private QueryParameters(List<Map.Entry<String, String>> pairs) {
        this.pairs = List.copyOf(pairs);
    }

    public static QueryParameters empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return pairs.isEmpty();
    }

    /** Return {@code base} with the encoded {@code ?k=v&...} appended, or {@code base} unchanged if empty. */
    String appendTo(String base) {
        if (pairs.isEmpty()) {
            return base;
        }
        StringBuilder builder = new StringBuilder(base);
        char separator = FIRST_SEPARATOR;
        for (Map.Entry<String, String> pair : pairs) {
            builder.append(separator)
                    .append(encode(pair.getKey()))
                    .append('=')
                    .append(encodeValue(pair.getValue()));
            separator = NEXT_SEPARATOR;
        }
        return builder.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Encode a parameter value, keeping the comma literal. A comma is a legal query sub-delimiter
     * (RFC 3986), and Erli's list parameters are documented in the {@code a,b,c} form, so encoding it
     * to {@code %2C} would diverge from the API's expected shape.
     */
    private static String encodeValue(String value) {
        return encode(value).replace(ENCODED_COMMA, CSV_DELIMITER);
    }

    /** Builder for {@link QueryParameters}. Every {@code add*} skips null/empty inputs. */
    public static final class Builder {

        private final List<Map.Entry<String, String>> pairs = new ArrayList<>();

        private Builder() {
        }

        /** A single {@code key=value} pair; skipped when {@code value} is null. */
        public Builder add(String key, String value) {
            if (value != null) {
                pairs.add(Map.entry(key, value));
            }
            return this;
        }

        /** A single boolean parameter; skipped when {@code value} is null. */
        public Builder addBoolean(String key, Boolean value) {
            return value == null ? this : add(key, value.toString());
        }

        /** A comma-joined list: {@code key=v1,v2,v3}; skipped when the collection is null or empty. */
        public Builder addCsv(String key, Collection<String> values) {
            if (values != null && !values.isEmpty()) {
                pairs.add(Map.entry(key, String.join(CSV_DELIMITER, values)));
            }
            return this;
        }

        /** Repeated keys: {@code key=v1&key=v2}; nulls within the collection are skipped. */
        public Builder addRepeated(String key, Collection<String> values) {
            if (values != null) {
                for (String value : values) {
                    if (value != null) {
                        pairs.add(Map.entry(key, value));
                    }
                }
            }
            return this;
        }

        /** Bracketed repeated keys: {@code key[]=v1&key[]=v2}. */
        public Builder addBracketRepeated(String key, Collection<String> values) {
            return addRepeated(key + BRACKET_SUFFIX, values);
        }

        public QueryParameters build() {
            return new QueryParameters(pairs);
        }
    }
}
