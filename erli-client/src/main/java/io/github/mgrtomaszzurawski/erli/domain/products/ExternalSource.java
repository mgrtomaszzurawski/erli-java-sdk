package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * Where an externally supplied attribute or category originated.
 */
public enum ExternalSource {

    /** Authored in the seller's own shop. */
    SHOP,

    /** Imported from Allegro. */
    ALLEGRO,

    /** Supplied by the Erli marketplace catalog. */
    MARKETPLACE
}
