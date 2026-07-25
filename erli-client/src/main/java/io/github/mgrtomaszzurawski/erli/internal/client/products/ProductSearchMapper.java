package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.domain.products.ComparisonOperator;
import io.github.mgrtomaszzurawski.erli.domain.products.Product;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFilter;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFilterField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSearchRequest;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSortField;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductFilterAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductFilterAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductFilterAnyOf1Value;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductFilterAnyOf2;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductFilterAnyOf3;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductFilterAnyOf4;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductFilterAnyOfValue;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductSearch;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductSearchPagination;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductSearchPaginationAfter;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the {@code POST /products/_search} request body and derives the pagination cursor.
 *
 * <p><strong>The cursor is client-side.</strong> Erli's product search answers with a bare JSON array —
 * unlike the other {@code _search} endpoints, there is no {@code pagination.after} in the response. The
 * request's {@code pagination.after} is a value of the <em>sort field</em>, and the server returns rows
 * strictly after it. So the next page's cursor is the sort-field value of the last row on this page, and
 * {@link #cursorOf} reads exactly that field. This is why the sort field and the cursor must stay in
 * step, and why changing the sort mid-walk would silently skip or repeat rows.
 *
 * <p>Internal: never exported.
 */
final class ProductSearchMapper {

    private ProductSearchMapper() {
    }

    static ProductSearch toRequest(ProductSearchRequest request) {
        ProductSearch raw = new ProductSearch();
        ProductSearchPagination pagination = new ProductSearchPagination();
        pagination.setSortField(ProductSearchPagination.SortFieldEnum
                .fromValue(ProductEnums.wireNameOf(request.sortField())));
        pagination.setOrder(ProductSearchPagination.OrderEnum
                .fromValue(ProductEnums.wireNameOf(request.order())));
        request.pageSize().ifPresent(pagination::setLimit);
        request.after().ifPresent(cursor -> pagination.setAfter(afterValue(cursor)));
        raw.pagination(pagination);
        request.filter().ifPresent(filter -> raw.setFilter(toRawFilter(filter)));
        // The generator pre-populates `fields` with ALL 58 selectable names, so leaving it alone would
        // send the full projection on every search — defeating the point of asking for fewer fields and
        // making each response as large as it can be. Null means "not stated", which is what an
        // unprojected search must say.
        if (request.fields().isEmpty()) {
            raw.setFields(null);
        } else {
            Set<String> fields = new LinkedHashSet<>();
            request.fields().forEach(field -> fields.add(ProductFieldNames.wireName(field)));
            raw.setFields(fields);
        }
        return raw;
    }

    /**
     * The cursor for the page after this one: the sort-field value of the last product returned. Empty
     * when the value is absent, which stops the walk rather than risking a repeat of the same page.
     */
    static Optional<Cursor> cursorOf(Product product, ProductSortField sortField) {
        Optional<String> value = switch (sortField) {
            case EXTERNAL_ID -> Optional.of(product.externalId().value());
            case MARKETPLACE_ID -> Optional.of(Long.toString(product.marketplaceId()));
            case NAME -> Optional.of(product.name());
            case EAN -> product.ean();
            case SKU -> product.sku();
            case CREATED -> Optional.of(product.created().toString());
            case UPDATED -> product.updated().map(Object::toString);
            case ARCHIVED_AT -> product.archivedAt().map(Object::toString);
        };
        return value.filter(text -> !text.isBlank()).map(Cursor::of);
    }

    /**
     * The cursor as the request's polymorphic {@code after}. It is sent as text: the SDK never parses a
     * cursor, and the marketplace compares it against the sort field's own type.
     */
    private static ProductSearchPaginationAfter afterValue(Cursor cursor) {
        return new ProductSearchPaginationAfter(cursor.value());
    }

    private static io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter toRawFilter(ProductFilter filter) {
        if (filter instanceof ProductFilter.Comparison comparison) {
            return comparisonFilter(comparison);
        }
        if (filter instanceof ProductFilter.Membership membership) {
            ProductFilterAnyOf2 raw = new ProductFilterAnyOf2();
            raw.setField(ProductFilterAnyOf2.FieldEnum.fromValue(filterFieldName(membership.field())));
            raw.setOperator(membership.included()
                    ? ProductFilterAnyOf2.OperatorEnum.IN
                    : ProductFilterAnyOf2.OperatorEnum.NIN);
            raw.setValue(membership.values());
            return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(raw);
        }
        if (filter instanceof ProductFilter.Junction junction) {
            ProductFilterAnyOf3 raw = new ProductFilterAnyOf3();
            raw.setOperator(junction.conjunction()
                    ? ProductFilterAnyOf3.OperatorEnum.AND
                    : ProductFilterAnyOf3.OperatorEnum.OR);
            raw.setValue(junction.operands().stream().map(ProductSearchMapper::toRawFilter).toList());
            return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(raw);
        }
        if (filter instanceof ProductFilter.Negation negation) {
            ProductFilterAnyOf4 raw = new ProductFilterAnyOf4();
            raw.setOperator(ProductFilterAnyOf4.OperatorEnum.NOT);
            raw.setValue(toRawFilter(negation.operand()));
            return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(raw);
        }
        throw new IllegalStateException("Unhandled ProductFilter variant: " + filter.getClass());
    }

    /**
     * Equality-only fields use the narrower {@code anyOf} branch; the marketplace rejects them on the
     * ordered-comparison branch even for {@code =}. {@link ProductFilter.Comparison} has already
     * guaranteed the pairing is legal, so this only has to pick the matching wire shape.
     */
    private static io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter comparisonFilter(
            ProductFilter.Comparison comparison) {
        String fieldName = filterFieldName(comparison.field());
        String operator = comparisonOperatorName(comparison.operator());
        if (comparison.field().supportsOrderedComparison()) {
            ProductFilterAnyOf raw = new ProductFilterAnyOf();
            raw.setField(ProductFilterAnyOf.FieldEnum.fromValue(fieldName));
            raw.setOperator(ProductFilterAnyOf.OperatorEnum.fromValue(operator));
            raw.setValue(new ProductFilterAnyOfValue(comparison.value()));
            return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(raw);
        }
        ProductFilterAnyOf1 raw = new ProductFilterAnyOf1();
        raw.setField(ProductFilterAnyOf1.FieldEnum.fromValue(fieldName));
        raw.setOperator(ProductFilterAnyOf1.OperatorEnum.fromValue(operator));
        raw.setValue(equalityValue(comparison.field(), comparison.value()));
        return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(raw);
    }

    /**
     * The equality branch's value is a boolean or a string on the wire. {@code archived} is the boolean
     * one, so its text is converted; everything else is sent verbatim. Sending {@code "true"} as a string
     * for {@code archived} makes the marketplace reject the filter.
     */
    private static ProductFilterAnyOf1Value equalityValue(ProductFilterField field, String value) {
        if (field == ProductFilterField.ARCHIVED) {
            return new ProductFilterAnyOf1Value(Boolean.valueOf(value));
        }
        return new ProductFilterAnyOf1Value(value);
    }

    private static String filterFieldName(ProductFilterField field) {
        return switch (field) {
            case EXTERNAL_ID -> "externalId";
            case MARKETPLACE_ID -> "marketplaceId";
            case NAME -> "name";
            case EAN -> "ean";
            case SKU -> "sku";
            case CREATED -> "created";
            case UPDATED -> "updated";
            case ARCHIVED_AT -> "archivedAt";
            case PRICE -> "price";
            case STOCK -> "stock";
            case CATEGORY_ID -> "categoryId";
            case TAX_RATE -> "taxRate";
            case EXTERNAL_REFERENCE_ID -> "externalReferenceId";
            case STATUS -> "status";
            case ARCHIVED -> "archived";
        };
    }

    private static String comparisonOperatorName(ComparisonOperator operator) {
        return switch (operator) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case GREATER_THAN -> ">";
            case GREATER_OR_EQUAL -> ">=";
            case LESS_THAN -> "<";
            case LESS_OR_EQUAL -> "<=";
        };
    }
}
