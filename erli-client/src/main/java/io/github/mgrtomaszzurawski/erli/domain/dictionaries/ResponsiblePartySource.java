package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

/**
 * Where a {@link ResponsibleParty} entry came from — the API itself, the shop panel, or an
 * integration such as Allegro or BaseLinker.
 *
 * <p>A value type rather than a Java enum: the list names third-party integrations and grows as
 * ERLI adds them.
 *
 * @param value the non-blank source identifier exactly as the API spells it
 */
public record ResponsiblePartySource(String value) {

    public static final ResponsiblePartySource API = new ResponsiblePartySource("api");
    public static final ResponsiblePartySource MANUAL = new ResponsiblePartySource("manual");

    public ResponsiblePartySource {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ResponsiblePartySource must not be null or blank");
        }
        value = value.trim();
    }

    public static ResponsiblePartySource of(String value) {
        return new ResponsiblePartySource(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
