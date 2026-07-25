package io.github.mgrtomaszzurawski.erli.internal;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Substitutes values into the templated paths in {@link ApiPaths}, percent-encoding each value as a
 * single URI path segment.
 *
 * <p>The encoding is not cosmetic. Identifiers reach the SDK from a consumer's own data — an order id
 * read out of their ERP, a seller-defined product id — so a raw {@code String.replace} into the
 * template lets those values change the request's shape. An id ending in {@code #} truncates
 * everything after it, which turns {@code PATCH /orders/{id}/status} into {@code PATCH /orders/{id}}:
 * a different, successful, authenticated write against the wrong resource. {@code ?} injects a query
 * string and {@code /} invents path segments.
 *
 * <p>{@link QueryParameters} already does the equivalent for the query string; this is the path-side
 * counterpart. Internal: never exported.
 */
public final class PathTemplate {

    private static final String UNRESERVED_PUNCTUATION = "-._~";
    private static final String CURRENT_DIRECTORY = ".";
    private static final String PARENT_DIRECTORY = "..";
    private static final char PERCENT = '%';
    private static final String HEX_DIGITS = "0123456789ABCDEF";
    private static final int HIGH_NIBBLE_SHIFT = 4;
    private static final int NIBBLE_MASK = 0x0F;
    private static final int BYTE_MASK = 0xFF;

    private PathTemplate() {
    }

    /**
     * Replace {@code placeholder} in {@code template} with {@code value}, encoded as one path segment.
     *
     * @param template    the templated path, e.g. {@code "/orders/{id}/status"}
     * @param placeholder the placeholder to replace, e.g. {@code "{id}"}
     * @param value       the raw value to substitute
     * @return the path with the encoded value substituted
     * @throws IllegalArgumentException if the value is blank or is a relative-path segment
     */
    public static String expand(String template, String placeholder, String value) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(placeholder, "placeholder");
        return template.replace(placeholder, encodeSegment(value));
    }

    /**
     * Percent-encode a single URI path segment: unreserved characters (RFC 3986) pass through and
     * everything else becomes {@code %XX} over its UTF-8 bytes.
     *
     * <p>Encoding alone cannot neutralize {@code .} and {@code ..} — both consist entirely of
     * unreserved characters, so they survive it and would still be resolved as relative path segments.
     * They are rejected outright instead.
     */
    private static String encodeSegment(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A path segment value must not be blank");
        }
        if (CURRENT_DIRECTORY.equals(value) || PARENT_DIRECTORY.equals(value)) {
            throw new IllegalArgumentException(
                    "A path segment value must not be a relative path segment, got: " + value);
        }
        StringBuilder encoded = new StringBuilder(value.length());
        for (byte rawByte : value.getBytes(StandardCharsets.UTF_8)) {
            int unsignedByte = rawByte & BYTE_MASK;
            if (isUnreserved(unsignedByte)) {
                encoded.append((char) unsignedByte);
            } else {
                encoded.append(PERCENT)
                        .append(HEX_DIGITS.charAt((unsignedByte >> HIGH_NIBBLE_SHIFT) & NIBBLE_MASK))
                        .append(HEX_DIGITS.charAt(unsignedByte & NIBBLE_MASK));
            }
        }
        return encoded.toString();
    }

    private static boolean isUnreserved(int unsignedByte) {
        char character = (char) unsignedByte;
        return (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9')
                || UNRESERVED_PUNCTUATION.indexOf(character) >= 0;
    }
}
