package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;
import java.util.Optional;

/**
 * Groups products that are variants of one another (size, colour, …). Products sharing a group id are
 * shown together on the offer page; {@code attributes} names which attributes vary within the group.
 *
 * @param id         the group identifier shared by all variants, when supplied
 * @param source     who established the grouping
 * @param attributes the identifiers of the varying attributes, as text (defensively copied)
 */
public record ExternalVariantGroup(
        Optional<String> id,
        Optional<VariantGroupSource> source,
        List<String> attributes) {

    public ExternalVariantGroup {
        attributes = List.copyOf(attributes);
    }
}
