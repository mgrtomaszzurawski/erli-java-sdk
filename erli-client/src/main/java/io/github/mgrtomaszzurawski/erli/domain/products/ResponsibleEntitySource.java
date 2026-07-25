package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The channel that supplied a responsible person or producer reference.
 */
public enum ResponsibleEntitySource {

    /** Supplied through the public API. */
    API("api"),

    /** Entered by hand in the shop panel. */
    MANUAL("manual"),

    /** Imported from Allegro. */
    ALLEGRO("allegro"),

    /** Imported from IdoSell. */
    IDOSELL("idosell"),

    /** Imported from PrestaShop. */
    PRESTA_SHOP("prestaShop"),

    /** Imported from Shoper. */
    SHOPER("shoper"),

    /** Imported from BaseLinker. */
    BASELINKER("baselinker");

    private final String wireName;

    ResponsibleEntitySource(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
