package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * An opaque pagination cursor — the {@code pagination.after} token echoed by the Erli
 * {@code POST .../_search} endpoints. It is never parsed by the SDK; it is handed back verbatim to
 * request the next page. Absence of a cursor (first or last page) is represented by the absence of a
 * {@code Cursor}, not by a blank value.
 *
 * @param value the non-blank cursor token
 */
public record Cursor(String value) {

    public Cursor {
        value = Identifiers.requireText(value, "Cursor");
    }

    public static Cursor of(String value) {
        return new Cursor(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
