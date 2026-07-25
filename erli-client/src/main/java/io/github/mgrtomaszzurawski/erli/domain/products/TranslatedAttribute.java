package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Optional;

/**
 * One attribute rendered in a translation's language.
 *
 * @param key    the attribute key it translates, when supplied
 * @param name   the translated attribute name, when supplied
 * @param values the translated values in their wire shape
 * @param unit   the translated unit of measure, when the attribute has one
 */
public record TranslatedAttribute(
        Optional<String> key,
        Optional<String> name,
        AttributeValues values,
        Optional<String> unit) {
}
