package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The shape of an external attribute's values; selects the {@link AttributeValues} variant.
 */
public enum ExternalAttributeType {

    /** Numeric values. */
    NUMBER("number"),

    /** A single from/to range. */
    RANGE("range"),

    /** References to dictionary entries carrying an id and a name. */
    DICTIONARY("dictionary"),

    /** Free-text values. */
    STRING("string");

    private final String wireName;

    ExternalAttributeType(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
