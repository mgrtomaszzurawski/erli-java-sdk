package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * The direction a paginated {@code _search} sorts its results. A write-only request parameter — the
 * SDK sends it but never decodes it back — so each constant carries only its {@link #wireValue()}.
 */
public enum SortOrder {

    /** Oldest / smallest first. */
    ASCENDING("ASC"),

    /** Newest / largest first. */
    DESCENDING("DESC");

    private final String wireValue;

    SortOrder(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The token Erli's {@code _search} endpoints expect for this direction. */
    public String wireValue() {
        return wireValue;
    }
}
