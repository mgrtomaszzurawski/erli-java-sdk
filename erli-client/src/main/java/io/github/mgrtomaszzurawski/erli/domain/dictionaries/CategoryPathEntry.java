package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import java.util.Objects;

/**
 * One step on a {@link Category}'s breadcrumb path.
 *
 * @param id the category identifier of this step
 * @param name the Polish category name of this step
 */
public record CategoryPathEntry(CategoryId id, String name) {

    public CategoryPathEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
