package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Identifier of a dictionary category. Owned by core; shared by Dictionaries and Products.
 *
 * @param value the non-blank category id
 */
public record CategoryId(String value) {

    public CategoryId {
        value = Identifiers.requireText(value, "CategoryId");
    }

    public static CategoryId of(String value) {
        return new CategoryId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
