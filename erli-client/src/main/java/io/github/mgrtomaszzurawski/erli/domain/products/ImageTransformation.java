package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A transformation the marketplace applied to a product image.
 */
public enum ImageTransformation {

    /** A watermark was removed. */
    CLEAN_WATERMARK("clean-watermark"),

    /** The image was processed by an AI pipeline. */
    AI("ai");

    private final String wireName;

    ImageTransformation(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
