package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Identifier of a category attribute. Owned by core; shared by Dictionaries and Products.
 *
 * @param value the non-blank attribute id
 */
public record AttributeId(String value) {

    public AttributeId {
        value = Identifiers.requireText(value, "AttributeId");
    }

    public static AttributeId of(String value) {
        return new AttributeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
