package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;

/**
 * One section of a structured product description — an ordered run of {@link DescriptionItem} blocks.
 *
 * @param items the blocks in this section, in display order (defensively copied)
 */
public record DescriptionSection(List<DescriptionItem> items) {

    public DescriptionSection {
        items = List.copyOf(items);
    }
}
