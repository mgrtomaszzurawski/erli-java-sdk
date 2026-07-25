package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * Where an externally supplied attribute or category originated.
 */
public enum ExternalSource {

    /** Authored in the seller's own shop. */
    SHOP("shop"),

    /** Imported from Allegro. */
    ALLEGRO("allegro"),

    /** Supplied by the Erli marketplace catalog. */
    MARKETPLACE("marketplace");

    private final String wireName;

    ExternalSource(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
