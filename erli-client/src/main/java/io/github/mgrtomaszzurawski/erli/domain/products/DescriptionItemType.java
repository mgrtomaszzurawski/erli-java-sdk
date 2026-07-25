package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The kind of block inside a structured description section.
 */
public enum DescriptionItemType {

    /** An HTML/text block carried in {@code content}. */
    TEXT,

    /** An image block carried in {@code url}. */
    IMAGE
}
