package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A transformation the marketplace applied to a product image.
 */
public enum ImageTransformation {

    /** A watermark was removed. */
    CLEAN_WATERMARK,

    /** The image was processed by an AI pipeline. */
    AI
}
