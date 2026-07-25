package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.domain.products.Product;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves a caller's {@code fields} selection into the projection actually sent to the marketplace.
 *
 * <p>{@link Product} is a total record: {@code externalId}, {@code name}, {@code price} and the rest of
 * the spec-required fields are plain components, not {@link java.util.Optional}, because a product
 * without them is not something a caller can act on. But Erli lets a projection omit <em>any</em> field,
 * including those — so a naive projection would come back missing the very fields the mapper needs and
 * fail with {@code IllegalStateException} instead of returning the smaller product the caller asked for.
 *
 * <p>So a projection is always widened by {@link #MAPPING_ESSENTIALS}. The caller still gets the payload
 * reduction they asked for — these ten are cheap — eight scalars plus two small objects — while the expensive parts of a product
 * (description, attributes, translations, images) remain excluded unless selected. Widening is silent by
 * design: it is the SDK keeping its own contract, not a choice the caller needs to make.
 *
 * <p>Internal: never exported.
 */
final class ProductProjection {

    /**
     * The fields {@link ProductMapper} requires to build a {@link Product}. Kept in step with the
     * {@code require(...)} calls there — adding a required component to {@code Product} means adding it
     * here, or projected reads start failing.
     */
    private static final Set<ProductField> MAPPING_ESSENTIALS = EnumSet.of(
            ProductField.EXTERNAL_ID,
            ProductField.MARKETPLACE_ID,
            ProductField.NAME,
            ProductField.SLUG,
            ProductField.STATUS,
            ProductField.STOCK,
            ProductField.PRICE,
            ProductField.DISPATCH_TIME,
            ProductField.FROZEN,
            ProductField.CREATED);

    private ProductProjection() {
    }

    /**
     * The caller's selection widened with {@link #MAPPING_ESSENTIALS}, in a stable order.
     *
     * @param selected the fields the caller asked for; must not be empty (an empty selection means
     *                 "no projection", which the callers handle before reaching here)
     */
    static Set<ProductField> widen(Set<ProductField> selected) {
        EnumSet<ProductField> projection = EnumSet.copyOf(selected);
        projection.addAll(MAPPING_ESSENTIALS);
        return projection;
    }

    /** The widened selection as the wire names Erli expects. */
    static List<String> wireNamesFor(Set<ProductField> selected) {
        return widen(selected).stream().map(ProductField::wireName).toList();
    }
}
