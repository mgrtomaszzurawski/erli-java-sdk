package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

/**
 * How an {@link Attribute}'s values are shaped. Unlike the carrier and country lists, this is a
 * closed structural taxonomy the SDK must branch on, so it is a real Java enum.
 */
public enum AttributeType {

    /** A single number; {@link Attribute#min()}, {@link Attribute#max()} and precision may apply. */
    NUMBER,

    /** A numeric range. */
    RANGE,

    /** One or more values drawn from the attribute's dictionary of allowed values. */
    DICTIONARY,

    /** Free text. */
    STRING
}
