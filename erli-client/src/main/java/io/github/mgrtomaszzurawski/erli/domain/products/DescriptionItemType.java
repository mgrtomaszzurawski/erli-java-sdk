package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The kind of block inside a structured description section.
 */
public enum DescriptionItemType {

    /** An HTML/text block carried in {@code content}. */
    TEXT("TEXT"),

    /** An image block carried in {@code url}. */
    IMAGE("IMAGE");

    private final String wireName;

    DescriptionItemType(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
