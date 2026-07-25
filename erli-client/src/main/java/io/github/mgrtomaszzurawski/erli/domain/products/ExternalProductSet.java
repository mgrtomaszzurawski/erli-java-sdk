package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;

/**
 * A bundle declared by the seller in their own identifiers; the marketplace resolves it into a
 * {@link ProductSet}.
 *
 * @param items the set's components (defensively copied)
 */
public record ExternalProductSet(List<ExternalProductSetItem> items) {

    public ExternalProductSet {
        items = List.copyOf(items);
    }
}
