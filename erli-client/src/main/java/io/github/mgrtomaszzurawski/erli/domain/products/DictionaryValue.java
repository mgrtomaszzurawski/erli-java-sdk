package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Optional;

/**
 * A reference to a marketplace dictionary entry: its identifier plus the human-readable name. Used by
 * {@link AttributeValues.DictionaryValues} and by external-category breadcrumbs.
 *
 * <p>The identifier is polymorphic on the wire (a string for shop-authored entries, a number for
 * marketplace ones), so it is kept as text and offered as a number when it parses.
 *
 * @param id   the dictionary entry identifier, as text
 * @param name the display name, when the marketplace supplied one
 */
public record DictionaryValue(String id, Optional<String> name) {
}
