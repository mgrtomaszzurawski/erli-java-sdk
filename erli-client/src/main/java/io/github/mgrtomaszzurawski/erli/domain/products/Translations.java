package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Optional;

/**
 * A product's translations, keyed by market. Erli currently translates into Polish and German.
 *
 * @param polish the Polish rendering, when present
 * @param german the German rendering, when present
 */
public record Translations(Optional<Translation> polish, Optional<Translation> german) {
}
