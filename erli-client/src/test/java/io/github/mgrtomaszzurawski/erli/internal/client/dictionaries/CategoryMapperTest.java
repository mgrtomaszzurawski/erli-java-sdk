package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.Category;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.CategoryResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryMapperTest {

    /**
     * Verbatim from the live sandbox: {@code POST /dictionaries/category/_search} with
     * {@code {"limit":5}}, 2026-07-25. Id 0 is the tree root and carries an empty breadcrumb.
     */
    private static final String OBSERVED_CATEGORIES_JSON = """
            [
              {"id":0,"name":"Korzeń","leaf":false,"breadcrumb":[]},
              {"id":1,"name":"Dom i Ogród","leaf":false,"breadcrumb":[{"id":1,"name":"Dom i Ogród"}]},
              {"id":4,"name":"Elementy dekarskie","leaf":true,
               "breadcrumb":[{"id":1,"name":"Dom i Ogród"},{"id":2,"name":"Budowa i Remont"},
                             {"id":3,"name":"Dachy"},{"id":4,"name":"Elementy dekarskie"}]}
            ]""";

    private static CategoryResponse[] decode(String json) {
        return new JsonCodec().read(json, CategoryResponse[].class);
    }

    private static List<Category> mapAll(String json) {
        return Arrays.stream(decode(json)).map(CategoryMapper::toDomain).toList();
    }

    @Test
    void mapsTheRootCategoryWithAnEmptyBreadcrumb() {
        Category root = mapAll(OBSERVED_CATEGORIES_JSON).get(0);

        assertEquals(CategoryId.of("0"), root.id());
        assertEquals("Korzeń", root.name());
        assertFalse(root.leaf());
        assertTrue(root.breadcrumb().isEmpty());
    }

    @Test
    void mapsALeafCategoryWithItsFullBreadcrumbPath() {
        Category leaf = mapAll(OBSERVED_CATEGORIES_JSON).get(2);

        assertEquals(CategoryId.of("4"), leaf.id());
        assertTrue(leaf.leaf());
        assertEquals(4, leaf.breadcrumb().size());
        assertEquals(CategoryId.of("1"), leaf.breadcrumb().get(0).id());
        assertEquals("Dom i Ogród", leaf.breadcrumb().get(0).name());
        assertEquals("Elementy dekarskie", leaf.breadcrumb().get(3).name());
    }

    @Test
    void exposesTheNumericIdUsedByThePaginationCursor() {
        Category leaf = mapAll(OBSERVED_CATEGORIES_JSON).get(2);

        assertEquals(4, CategoryMapper.numericId(leaf.id()));
    }

    @Test
    void rejectsANonNumericIdAsAPaginationCursor() {
        CategoryId nonNumericId = CategoryId.of("not-a-number");
        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> CategoryMapper.numericId(nonNumericId));

        assertTrue(failure.getMessage().contains("not-a-number"), failure.getMessage());
    }

    @Test
    void rejectsACategoryMissingASpecRequiredField() {
        CategoryResponse[] raw = decode("[{\"id\":1,\"leaf\":true,\"breadcrumb\":[]}]");

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> CategoryMapper.toDomain(raw[0]));

        assertTrue(failure.getMessage().contains("name"), failure.getMessage());
    }

    @Test
    void mapsAnEmptyPayloadToAnEmptyList() {
        assertTrue(mapAll("[]").isEmpty());
    }

    @Test
    void returnsAnImmutableBreadcrumb() {
        List<Category> categories = mapAll(OBSERVED_CATEGORIES_JSON);

        var breadcrumb = categories.get(2).breadcrumb();
        assertThrows(UnsupportedOperationException.class, () -> breadcrumb.clear());
    }
}
