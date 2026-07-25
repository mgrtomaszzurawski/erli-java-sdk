package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;

/**
 * A product's structured description: an ordered list of {@link DescriptionSection}s, each holding text
 * and image blocks. Erli renders these sections in order on the offer page.
 *
 * @param sections the sections in display order (defensively copied)
 */
public record ProductDescription(List<DescriptionSection> sections) {

    public ProductDescription {
        sections = List.copyOf(sections);
    }
}
