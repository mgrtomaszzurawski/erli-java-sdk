package io.github.mgrtomaszzurawski.erli.core.model;

/** Shared validation for the typed-identifier value objects in this package. */
final class Identifiers {

    private Identifiers() {
    }

    /** Require a non-null, non-blank identifier value and return it trimmed. */
    static String requireText(String value, String typeName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(typeName + " must not be null or blank");
        }
        return value.trim();
    }
}
