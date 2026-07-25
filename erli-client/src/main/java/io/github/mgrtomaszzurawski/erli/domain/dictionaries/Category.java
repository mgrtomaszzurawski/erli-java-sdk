package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;

import java.util.List;
import java.util.Objects;

/**
 * A node in the marketplace category tree. Only a {@link #leaf()} category can hold products, and
 * attributes are defined per leaf category — see {@link DictionaryAccess#attributes(CategoryId)}.
 *
 * @param id the category identifier
 * @param name the Polish category name
 * @param leaf whether products may be listed directly in this category
 * @param breadcrumb the path from the tree root to this category, root first; empty for the root
 */
public record Category(CategoryId id, String name, boolean leaf, List<CategoryPathEntry> breadcrumb) {

    public Category {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        breadcrumb = List.copyOf(Objects.requireNonNull(breadcrumb, "breadcrumb"));
    }
}
