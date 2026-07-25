package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.Locale;

/**
 * An ISO 3166-1 alpha-2 country code as the Erli API spells it — lower case, e.g. {@code pl}.
 *
 * <p>A value type rather than a 249-constant Java enum: the country list is reference data, and an
 * enum of that size buys no type safety a two-letter check does not already give.
 *
 * @param value the lower-case two-letter country code
 */
public record CountryCode(String value) {

    public static final CountryCode POLAND = new CountryCode("pl");
    public static final CountryCode GERMANY = new CountryCode("de");

    private static final int ISO_ALPHA2_LENGTH = 2;

    public CountryCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CountryCode must not be null or blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() != ISO_ALPHA2_LENGTH) {
            throw new IllegalArgumentException(
                    "CountryCode must be an ISO 3166-1 alpha-2 code, got: " + value);
        }
    }

    public static CountryCode of(String value) {
        return new CountryCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
