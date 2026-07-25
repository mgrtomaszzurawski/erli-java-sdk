package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The marketplace or comparison site an external product reference points at.
 */
public enum ExternalReferenceKind {

    /** Allegro. */
    ALLEGRO,

    /** Ceneo. */
    CENEO,

    /** Amazon. */
    AMAZON,

    /** Empik. */
    EMPIK,

    /** Morele. */
    MORELE,

    /** Arena. */
    ARENA,

    /** Another site not covered by the named kinds. */
    OTHER,

    /** A seller-defined reference. */
    CUSTOM
}
