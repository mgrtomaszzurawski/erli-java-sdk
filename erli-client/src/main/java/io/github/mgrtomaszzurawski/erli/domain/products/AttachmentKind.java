package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The kind of document attached to a product.
 */
public enum AttachmentKind {

    /** A general guide. */
    GUIDE("guide"),

    /** The rules of a promotion. */
    PROMOTION_RULES("promotionRules"),

    /** The rules of a contest. */
    CONTEST_RULES("contestRules"),

    /** A sample fragment of a book. */
    BOOK_SNIPPET("bookSnippet"),

    /** A user manual. */
    USER_MANUAL("userManual"),

    /** Assembly instructions. */
    ASSEMBLY_INSTRUCTIONS("assemblyInstructions"),

    /** Game instructions. */
    GAME_INSTRUCTIONS("gameInstructions"),

    /** A safety guide. */
    SAFETY_GUIDE("safetyGuide"),

    /** The EU energy label. */
    ENERGY_LABEL("energyLabel"),

    /** The product information card. */
    PRODUCT_CARD("productCard"),

    /** The EU tyre label. */
    TIRE_LABEL("tireLabel"),

    /** Software data-processing information. */
    DATA_PROCESSING_SOFTWARE("dataProcessingSoftware"),

    /** Hardware data-processing information. */
    DATA_PROCESSING_HARDWARE("dataProcessingHardware"),

    /** A safety data sheet. */
    SAFETY_DATA_SHEET("safetyDataSheet"),

    /** A plant-protection product licence. */
    PLANT_PROTECTION_LICENSE("plantProtectionLicense"),

    /** Recycling information. */
    RECYCLING_INFO("recyclingInfo");

    private final String wireName;

    AttachmentKind(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
