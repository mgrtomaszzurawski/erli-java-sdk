package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.domain.products.FilterValueKind;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Optional;

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

    private static final String BOOLEAN_TRUE = "true";
    private static final String BOOLEAN_FALSE = "false";

    private ProductSearchMapper() {
    }

    static ProductSearch toRequest(ProductSearchRequest request) {
        ProductSearch rawSearch = new ProductSearch();
        ProductSearchPagination pagination = new ProductSearchPagination();
        pagination.setSortField(ProductSearchPagination.SortFieldEnum
                .fromValue(request.sortField().wireName()));
        pagination.setOrder(ProductSearchPagination.OrderEnum
                .fromValue(request.order().wireValue()));
        // Always state the limit. The walk decides it has reached the last page by comparing a page
        // against the size it asked for, so leaving the server to apply its own default would silently
        // truncate every search the day that default changes.
        pagination.setLimit(request.effectivePageSize());
        request.after().ifPresent(cursor -> pagination.setAfter(afterValue(cursor, request.sortField())));
        rawSearch.pagination(pagination);
        request.filter().ifPresent(filter -> rawSearch.setFilter(toRawFilter(filter)));
        // The generator pre-populates `fields` with ALL 58 selectable names, so leaving it alone would
        // send the full projection on every search — defeating the point of asking for fewer fields and
        // making each response as large as it can be. Null means "not stated", which is what an
        // unprojected search must say; a stated projection is widened so every row stays mappable.
        if (request.fields().isEmpty()) {
            rawSearch.setFields(null);
        } else {
            rawSearch.setFields(new LinkedHashSet<>(ProductProjection.wireNamesFor(request.fields())));
        }
        return rawSearch;
    }

    /**
     * The cursor for the page after this one: the sort-field value of the last product returned. Empty
     * when that row has no value for the field, in which case there is nothing to page from.
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
     * The cursor as the request's polymorphic {@code after}, in the JSON type the sort field takes.
     *
     * <p>The SDK does not interpret a cursor's meaning, but it must state its type: {@code after} is
     * compared against the sort column, so a numeric column given a quoted value does not compare and
     * the walk ends after one page. A cursor supplied by the caller to resume a persisted walk is
     * therefore parsed here, and a malformed one is reported against the sort field rather than
     * escaping as a bare parse error.
     */
    private static ProductSearchPaginationAfter afterValue(Cursor cursor, ProductSortField sortField) {
        if (sortField.cursorKind() == FilterValueKind.NUMBER) {
            return new ProductSearchPaginationAfter(parseNumber(sortField, cursor.value()));
        }
        if (sortField.cursorKind() == FilterValueKind.DATE_TIME) {
            return new ProductSearchPaginationAfter(parseTimestamp(sortField, cursor.value()));
        }
        return new ProductSearchPaginationAfter(cursor.value());
    }

    private static io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter toRawFilter(ProductFilter filter) {
        if (filter instanceof ProductFilter.Comparison comparison) {
            return comparisonFilter(comparison);
        }
        if (filter instanceof ProductFilter.Membership membership) {
            ProductFilterAnyOf2 rawFilter = new ProductFilterAnyOf2();
            rawFilter.setField(ProductFilterAnyOf2.FieldEnum.fromValue(membership.field().wireName()));
            rawFilter.setOperator(membership.included()
                    ? ProductFilterAnyOf2.OperatorEnum.IN
                    : ProductFilterAnyOf2.OperatorEnum.NIN);
            rawFilter.setValue(membership.values().stream()
                    .map(value -> typedValue(membership.field(), value))
                    .toList());
            return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(rawFilter);
        }
        if (filter instanceof ProductFilter.Junction junction) {
            ProductFilterAnyOf3 rawFilter = new ProductFilterAnyOf3();
            rawFilter.setOperator(junction.conjunction()
                    ? ProductFilterAnyOf3.OperatorEnum.AND
                    : ProductFilterAnyOf3.OperatorEnum.OR);
            rawFilter.setValue(junction.operands().stream().map(ProductSearchMapper::toRawFilter).toList());
            return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(rawFilter);
        }
        if (filter instanceof ProductFilter.Negation negation) {
            ProductFilterAnyOf4 rawFilter = new ProductFilterAnyOf4();
            rawFilter.setOperator(ProductFilterAnyOf4.OperatorEnum.NOT);
            rawFilter.setValue(toRawFilter(negation.operand()));
            return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(rawFilter);
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
        String fieldName = comparison.field().wireName();
        String operator = comparison.operator().wireName();
        if (comparison.field().supportsOrderedComparison()) {
            ProductFilterAnyOf rawFilter = new ProductFilterAnyOf();
            rawFilter.setField(ProductFilterAnyOf.FieldEnum.fromValue(fieldName));
            rawFilter.setOperator(ProductFilterAnyOf.OperatorEnum.fromValue(operator));
            rawFilter.setValue(orderedValue(comparison.field(), comparison.value()));
            return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(rawFilter);
        }
        ProductFilterAnyOf1 rawFilter = new ProductFilterAnyOf1();
        rawFilter.setField(ProductFilterAnyOf1.FieldEnum.fromValue(fieldName));
        rawFilter.setOperator(ProductFilterAnyOf1.OperatorEnum.fromValue(operator));
        rawFilter.setValue(equalityValue(comparison.field(), comparison.value()));
        return new io.github.mgrtomaszzurawski.erli.rest.model.ProductFilter(rawFilter);
    }

    /**
     * The equality branch's value is a boolean or a string on the wire. {@code archived} is the boolean
     * one, so its text is converted; everything else is sent verbatim. Sending {@code "true"} as a string
     * for {@code archived} makes the marketplace reject the filter.
     */
    private static ProductFilterAnyOf1Value equalityValue(ProductFilterField field, String value) {
        if (field.valueKind() == FilterValueKind.BOOLEAN) {
            return new ProductFilterAnyOf1Value(parseBoolean(field, value));
        }
        return new ProductFilterAnyOf1Value(value);
    }

    /**
     * The ordered branch's value, in the JSON type the field takes. Erli compares a filter value against
     * the column's own type, so a numeric field given {@code "0"} rather than {@code 0} simply does not
     * match — a wrong answer with no error, which is exactly what the SDK exists to prevent.
     */
    private static ProductFilterAnyOfValue orderedValue(ProductFilterField field, String value) {
        if (field.valueKind() == FilterValueKind.NUMBER) {
            return new ProductFilterAnyOfValue(parseNumber(field, value));
        }
        if (field.valueKind() == FilterValueKind.DATE_TIME) {
            return new ProductFilterAnyOfValue(parseTimestamp(field, value));
        }
        return new ProductFilterAnyOfValue(value);
    }

    /**
     * A membership entry in the field's own JSON type; the branch is free-form at Layer 1. Validated the
     * same way as a comparison value — a set filter is no less type-sensitive than a scalar one.
     */
    private static Object typedValue(ProductFilterField field, String value) {
        return switch (field.valueKind()) {
            case NUMBER -> parseNumber(field, value);
            case DATE_TIME -> parseTimestamp(field, value);
            case BOOLEAN -> parseBoolean(field, value);
            case TEXT -> value;
        };
    }

    private static BigDecimal parseNumber(Enum<?> field, String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException notNumeric) {
            throw new IllegalArgumentException(
                    "'" + field + "' takes a number, but '" + value + "' is not one",
                    notNumeric);
        }
    }

    private static OffsetDateTime parseTimestamp(Enum<?> field, String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException notATimestamp) {
            throw new IllegalArgumentException(
                    "'" + field + "' takes a timestamp, but '" + value
                            + "' is not an ISO-8601 instant", notATimestamp);
        }
    }

    /**
     * A boolean filter value. Rejects anything other than {@code true}/{@code false} rather than letting
     * {@link Boolean#valueOf} turn a typo into {@code false} and return the wrong products.
     */
    private static boolean parseBoolean(Enum<?> field, String value) {
        if (BOOLEAN_TRUE.equalsIgnoreCase(value)) {
            return true;
        }
        if (BOOLEAN_FALSE.equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(
                "'" + field + "' takes a boolean, but '" + value + "' is neither"
                        + " '" + BOOLEAN_TRUE + "' nor '" + BOOLEAN_FALSE + "'");
    }


}
