package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.internal.QueryParameters;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Builds concrete request paths for the products endpoints from the {@code ApiPaths} templates.
 *
 * <p>A product's {@code externalId} is seller-assigned free text, so it can legitimately contain
 * {@code /}, {@code ?}, {@code #} or spaces. Substituting it into the path unescaped would let it change
 * the request's shape, so it is percent-encoded here. {@link URLEncoder} targets
 * {@code application/x-www-form-urlencoded}, which differs from path encoding in one respect — it emits
 * {@code +} for a space — so that one substitution is corrected to {@code %20}.
 *
 * <p>Query parameters are <em>not</em> built here: the shared transport owns that
 * ({@link QueryParameters}), and duplicating it in a bucket would fork the encoding rules.
 * Internal: never exported.
 */
final class ProductPaths {

    private static final String EXTERNAL_ID_PLACEHOLDER = "{externalId}";
    private static final String FORM_ENCODED_SPACE = "+";
    private static final String PATH_ENCODED_SPACE = "%20";
    private static final String FIELDS_PARAMETER = "fields";

    private ProductPaths() {
    }

    /** Substitute a percent-encoded {@code externalId} into a path template. */
    static String withExternalId(String template, ProductExternalId externalId) {
        return template.replace(EXTERNAL_ID_PLACEHOLDER, encodePathSegment(externalId.value()));
    }

    /**
     * The {@code fields} projection as query parameters, or empty when nothing is selected (which asks
     * the marketplace for the whole product). Erli takes the selection as one comma-joined value.
     */
    static QueryParameters fieldsQuery(Set<ProductField> fields) {
        return QueryParameters.builder()
                .addCsv(FIELDS_PARAMETER, fields.stream().map(ProductFieldNames::wireName).toList())
                .build();
    }

    private static String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace(FORM_ENCODED_SPACE, PATH_ENCODED_SPACE);
    }
}
