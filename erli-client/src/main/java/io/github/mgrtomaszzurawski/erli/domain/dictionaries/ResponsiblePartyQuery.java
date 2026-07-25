package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

/**
 * Optional server-side filters for the responsible-person and responsible-producer dictionaries.
 * A {@code null} component means "do not filter on this"; use {@link #none()} for every entry.
 *
 * @param id keep only the entry with this identifier
 * @param name keep only entries whose name contains this fragment, case-insensitively
 */
public record ResponsiblePartyQuery(Long id, String name) {

    private static final ResponsiblePartyQuery NONE = new ResponsiblePartyQuery(null, null);

    /** The empty query — every entry. */
    public static ResponsiblePartyQuery none() {
        return NONE;
    }

    /** Filter by identifier. */
    public static ResponsiblePartyQuery byId(long id) {
        return new ResponsiblePartyQuery(id, null);
    }

    /** Filter by a case-insensitive fragment of the name. */
    public static ResponsiblePartyQuery byName(String nameFragment) {
        return new ResponsiblePartyQuery(null, nameFragment);
    }
}
