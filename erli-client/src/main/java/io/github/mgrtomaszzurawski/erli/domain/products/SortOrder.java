package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The direction a product search is sorted in.
 */
public enum SortOrder {

    /** Ascending. */
    ASC("ASC"),

    /** Descending. */
    DESC("DESC");

    private final String wireName;

    SortOrder(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
