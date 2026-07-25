package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * The time unit of a product's dispatch window.
 */
public enum DispatchTimeUnit {

    /** Hours. */
    HOUR("hour"),

    /** Days. */
    DAY("day"),

    /** Months. */
    MONTH("month");

    private final String wireName;

    DispatchTimeUnit(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
