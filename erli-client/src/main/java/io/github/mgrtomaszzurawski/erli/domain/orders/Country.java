package io.github.mgrtomaszzurawski.erli.domain.orders;

/**
 * Country of a buyer address. Erli's address schema currently enumerates Poland only; the enum exists
 * so that widening the list upstream becomes a compile error in the mapper rather than a silent
 * runtime surprise.
 */
public enum Country {

    /** Poland. */
    PL
}
