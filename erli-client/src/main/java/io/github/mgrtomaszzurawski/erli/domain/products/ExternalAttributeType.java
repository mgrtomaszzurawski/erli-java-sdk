package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The shape of an external attribute's values; selects the {@link AttributeValues} variant.
 */
public enum ExternalAttributeType {

    /** Numeric values. */
    NUMBER,

    /** A single from/to range. */
    RANGE,

    /** References to dictionary entries carrying an id and a name. */
    DICTIONARY,

    /** Free-text values. */
    STRING
}
