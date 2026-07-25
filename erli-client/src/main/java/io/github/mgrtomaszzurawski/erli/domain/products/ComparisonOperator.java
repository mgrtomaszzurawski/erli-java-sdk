package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * How a {@link ProductFilter} compares a field to a value.
 */
public enum ComparisonOperator {

    /** Equal to. Available on every {@link ProductFilterField}. */
    EQUALS("=", false),

    /** Not equal to. Available on every {@link ProductFilterField}. */
    NOT_EQUALS("!=", false),

    /** Strictly greater than. Ordered fields only. */
    GREATER_THAN(">", true),

    /** Greater than or equal to. Ordered fields only. */
    GREATER_OR_EQUAL(">=", true),

    /** Strictly less than. Ordered fields only. */
    LESS_THAN("<", true),

    /** Less than or equal to. Ordered fields only. */
    LESS_OR_EQUAL("<=", true);

    private final String wireName;
    private final boolean requiresOrdering;

    ComparisonOperator(String wireName, boolean requiresOrdering) {
        this.wireName = wireName;
        this.requiresOrdering = requiresOrdering;
    }

    /** The symbol the Erli API uses for this operator on the wire. */
    public String wireName() {
        return wireName;
    }

    /** Whether this operator needs a field that supports ordered comparison. */
    public boolean requiresOrderedField() {
        return requiresOrdering;
    }
}
