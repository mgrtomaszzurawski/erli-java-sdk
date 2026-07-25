package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The channel that supplied a responsible person or producer reference.
 */
public enum ResponsibleEntitySource {

    /** Supplied through the public API. */
    API,

    /** Entered by hand in the shop panel. */
    MANUAL,

    /** Imported from Allegro. */
    ALLEGRO,

    /** Imported from IdoSell. */
    IDOSELL,

    /** Imported from PrestaShop. */
    PRESTA_SHOP,

    /** Imported from Shoper. */
    SHOPER,

    /** Imported from BaseLinker. */
    BASELINKER
}
