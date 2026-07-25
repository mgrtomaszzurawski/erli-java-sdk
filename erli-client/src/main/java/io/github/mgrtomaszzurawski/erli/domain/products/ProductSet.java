package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.List;

/**
 * A bundle expressed in marketplace meta-product ids — the resolved counterpart of an
 * {@link ExternalProductSet}.
 *
 * @param items the set's components (defensively copied)
 */
public record ProductSet(List<ProductSetItem> items) {

    public ProductSet {
        items = List.copyOf(items);
    }
}
