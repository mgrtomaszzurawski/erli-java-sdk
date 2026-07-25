package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;
import java.util.Optional;

/**
 * A seller- or integration-supplied category path, before the marketplace matches it against its own
 * category tree (the matched result appears as a {@link ProductCategory} path).
 *
 * @param source     where the category came from
 * @param breadcrumb the category path from root to leaf (defensively copied)
 * @param index      the display position among the product's categories, when supplied
 */
public record ExternalCategory(
        Optional<ExternalSource> source,
        List<DictionaryValue> breadcrumb,
        Optional<Integer> index) {

    public ExternalCategory {
        breadcrumb = List.copyOf(breadcrumb);
    }
}
