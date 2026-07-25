package io.github.mgrtomaszzurawski.erli.domain.shipping;

/**
 * Comparison used by a parcel search filter. The API splits these across two request shapes — the
 * scalar comparisons take one value, {@link #IN} and {@link #NOT_IN} take a list — which
 * {@link ParcelFilter} keeps apart at the type level so an impossible pairing cannot be built.
 */
public enum ParcelSearchOperator {

    /** Equal to. */
    EQUAL("="),
    /** Not equal to. */
    NOT_EQUAL("!="),
    /** Greater than. */
    GREATER_THAN(">"),
    /** Greater than or equal to. */
    GREATER_THAN_OR_EQUAL(">="),
    /** Less than. */
    LESS_THAN("<"),
    /** Less than or equal to. */
    LESS_THAN_OR_EQUAL("<="),
    /** One of a list of values. */
    IN("in"),
    /** None of a list of values. */
    NOT_IN("nin");

    private final String wireValue;

    ParcelSearchOperator(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this operator is sent as on the wire. */
    public String wireValue() {
        return wireValue;
    }
}
