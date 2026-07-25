package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The kind of document attached to a product.
 */
public enum AttachmentKind {

    /** A general guide. */
    GUIDE,

    /** The rules of a promotion. */
    PROMOTION_RULES,

    /** The rules of a contest. */
    CONTEST_RULES,

    /** A sample fragment of a book. */
    BOOK_SNIPPET,

    /** A user manual. */
    USER_MANUAL,

    /** Assembly instructions. */
    ASSEMBLY_INSTRUCTIONS,

    /** Game instructions. */
    GAME_INSTRUCTIONS,

    /** A safety guide. */
    SAFETY_GUIDE,

    /** The EU energy label. */
    ENERGY_LABEL,

    /** The product information card. */
    PRODUCT_CARD,

    /** The EU tyre label. */
    TIRE_LABEL,

    /** Software data-processing information. */
    DATA_PROCESSING_SOFTWARE,

    /** Hardware data-processing information. */
    DATA_PROCESSING_HARDWARE,

    /** A safety data sheet. */
    SAFETY_DATA_SHEET,

    /** A plant-protection product licence. */
    PLANT_PROTECTION_LICENSE,

    /** Recycling information. */
    RECYCLING_INFO
}
