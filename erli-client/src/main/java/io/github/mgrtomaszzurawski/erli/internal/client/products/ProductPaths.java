package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.internal.PathTemplate;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;
import java.util.Map;
import java.util.Set;

/**
 * Builds concrete request paths for the products endpoints from the {@code ApiPaths} templates.
 *
 * <p>Neither path encoding nor query encoding is implemented here: core owns both
 * ({@link PathTemplate}, {@link QueryParameters}). This class only knows the products-specific part —
 * which placeholder the id fills and what the {@code fields} parameter is called.
 *
 * <p>The encoding matters and is core's job to get right: a product's {@code externalId} is
 * seller-assigned free text, so it can legitimately contain {@code /}, {@code ?} or a {@code ..}
 * segment, and substituting it raw would let it re-route the request to a different resource.
 * Internal: never exported.
 */
final class ProductPaths {

    private static final String EXTERNAL_ID_PLACEHOLDER_NAME = "externalId";
    private static final String FIELDS_PARAMETER = "fields";

    private ProductPaths() {
    }

    /** Expand a products path template with the given (core-encoded) {@code externalId}. */
    static String withExternalId(String template, ProductExternalId externalId) {
        return PathTemplate.expand(template, Map.of(EXTERNAL_ID_PLACEHOLDER_NAME, externalId.value()));
    }

    /**
     * The {@code fields} projection as query parameters, or empty when nothing is selected (which asks
     * the marketplace for the whole product). Erli takes the selection as one comma-joined value.
     */
    static QueryParameters fieldsQuery(Set<ProductField> fields) {
        if (fields.isEmpty()) {
            return QueryParameters.empty();
        }
        return QueryParameters.builder()
                .addCsv(FIELDS_PARAMETER, ProductProjection.wireNamesFor(fields))
                .build();
    }
}
