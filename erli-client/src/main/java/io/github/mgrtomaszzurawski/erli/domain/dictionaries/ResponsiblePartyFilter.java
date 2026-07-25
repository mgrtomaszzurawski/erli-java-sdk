package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.Objects;
import java.util.Optional;

/**
 * Optional server-side filters for the responsible-person and responsible-producer dictionaries.
 *
 * @param id keep only the entry with this identifier
 * @param name keep only entries whose name contains this fragment, case-insensitively
 */
public record ResponsiblePartyFilter(Optional<Long> id, Optional<String> name) {

    private static final ResponsiblePartyFilter ALL =
            new ResponsiblePartyFilter(Optional.empty(), Optional.empty());

    public ResponsiblePartyFilter {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    /** The empty filter — every entry. */
    public static ResponsiblePartyFilter all() {
        return ALL;
    }

    /** Filter by identifier. */
    public static ResponsiblePartyFilter byId(long id) {
        return new ResponsiblePartyFilter(Optional.of(id), Optional.empty());
    }

    /** Filter by a case-insensitive fragment of the name. */
    public static ResponsiblePartyFilter byName(String nameFragment) {
        return new ResponsiblePartyFilter(
                Optional.empty(), Optional.of(Objects.requireNonNull(nameFragment, "nameFragment")));
    }
}
