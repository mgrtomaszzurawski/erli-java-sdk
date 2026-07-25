package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * The name that identifies a webhook subscription (Erli addresses hooks by {@code hookName}). Owned
 * by core; used by the Comms &amp; Automation bucket.
 *
 * @param value the non-blank hook name
 */
public record HookName(String value) {

    public HookName {
        value = Identifiers.requireText(value, "HookName");
    }

    public static HookName of(String value) {
        return new HookName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
