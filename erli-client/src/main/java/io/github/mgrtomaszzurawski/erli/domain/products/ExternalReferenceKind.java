package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The marketplace or comparison site an external product reference points at.
 */
public enum ExternalReferenceKind {

    /** Allegro. */
    ALLEGRO("allegro"),

    /** Ceneo. */
    CENEO("ceneo"),

    /** Amazon. */
    AMAZON("amazon"),

    /** Empik. */
    EMPIK("empik"),

    /** Morele. */
    MORELE("morele"),

    /** Arena. */
    ARENA("arena"),

    /** Another site not covered by the named kinds. */
    OTHER("other"),

    /** A seller-defined reference. */
    CUSTOM("custom");

    private final String wireName;

    ExternalReferenceKind(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
