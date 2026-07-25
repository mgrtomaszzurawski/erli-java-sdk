package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Category;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.CategoryPathEntry;
import io.github.mgrtomaszzurawski.erli.rest.model.CategoryAttributeResponseBreadcrumbInner;
import io.github.mgrtomaszzurawski.erli.rest.model.CategoryResponse;
import java.util.List;
import java.util.Objects;

/**
 * Maps the generated Layer-1 {@link CategoryResponse} to the public {@link Category} domain record.
 *
 * <p>Category identifiers are integers on the wire but {@link CategoryId} is core-owned and
 * string-valued, so they are carried as their decimal text. {@link #numericId} converts back for the
 * {@code after} pagination cursor, which the API requires as an integer. Internal: never exported.
 */
final class CategoryMapper {

    private CategoryMapper() {
    }

    static Category toDomain(CategoryResponse rawCategory) {
        Objects.requireNonNull(rawCategory, "raw CategoryResponse");
        return new Category(
                CategoryId.of(String.valueOf(requireId(rawCategory))),
                requireName(rawCategory),
                requireLeaf(rawCategory),
                toBreadcrumb(rawCategory));
    }

    /**
     * The numeric form of a category id, for the {@code after} cursor.
     *
     * @throws IllegalStateException if the id is not an integer, which would mean the API changed shape
     */
    static int numericId(CategoryId categoryId) {
        try {
            return Integer.parseInt(categoryId.value());
        } catch (NumberFormatException notNumeric) {
            throw new IllegalStateException(
                    "Category id is not numeric and cannot be used as a pagination cursor: "
                            + categoryId.value(), notNumeric);
        }
    }

    private static int requireId(CategoryResponse rawCategory) {
        Integer id = rawCategory.getId();
        if (id == null) {
            throw new IllegalStateException("CategoryResponse is missing the required 'id' field");
        }
        return id;
    }

    private static String requireName(CategoryResponse rawCategory) {
        String name = rawCategory.getName();
        if (name == null) {
            throw new IllegalStateException("CategoryResponse is missing the required 'name' field");
        }
        return name;
    }

    private static boolean requireLeaf(CategoryResponse rawCategory) {
        Boolean leaf = rawCategory.getLeaf();
        if (leaf == null) {
            throw new IllegalStateException("CategoryResponse is missing the required 'leaf' field");
        }
        return leaf;
    }

    private static List<CategoryPathEntry> toBreadcrumb(CategoryResponse rawCategory) {
        List<CategoryAttributeResponseBreadcrumbInner> breadcrumb = rawCategory.getBreadcrumb();
        if (breadcrumb == null) {
            return List.of();
        }
        return breadcrumb.stream().map(CategoryMapper::toPathEntry).toList();
    }

    private static CategoryPathEntry toPathEntry(CategoryAttributeResponseBreadcrumbInner rawEntry) {
        Integer id = rawEntry.getId();
        String name = rawEntry.getName();
        if (id == null || name == null) {
            throw new IllegalStateException(
                    "Category breadcrumb entry is missing a required 'id' or 'name' field");
        }
        return new CategoryPathEntry(CategoryId.of(String.valueOf(id)), name);
    }
}
