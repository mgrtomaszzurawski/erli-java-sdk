package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds concrete request paths for the products endpoints from the {@code ApiPaths} templates.
 *
 * <p>A product's {@code externalId} is seller-assigned free text, so it can legitimately contain
 * {@code /}, {@code ?}, {@code #} or spaces. Substituting it into the path unescaped would let it change
 * the request's shape, so it is percent-encoded here. {@link java.net.URLEncoder} targets
 * {@code application/x-www-form-urlencoded}, which differs from path encoding in one respect — it emits
 * {@code +} for a space — so that one substitution is corrected to {@code %20}.
 *
 * <p>Internal: never exported. This is products-local because the shared transport does not yet own URI
 * building; see BACKLOG item CORE-1.
 */
final class ProductPaths {

    private static final String EXTERNAL_ID_PLACEHOLDER = "{externalId}";
    private static final String FORM_ENCODED_SPACE = "+";
    private static final String PATH_ENCODED_SPACE = "%20";
    private static final String FIELDS_QUERY_PREFIX = "?fields=";
    private static final String FIELDS_SEPARATOR = ",";

    private ProductPaths() {
    }

    /** Substitute a percent-encoded {@code externalId} into a path template. */
    static String withExternalId(String template, ProductExternalId externalId) {
        return template.replace(EXTERNAL_ID_PLACEHOLDER, encodePathSegment(externalId.value()));
    }

    /**
     * Append the {@code fields} projection to a path, or return it unchanged when nothing is selected
     * (which asks the marketplace for the whole product).
     */
    static String withFields(String path, Set<ProductField> fields) {
        if (fields.isEmpty()) {
            return path;
        }
        return path + FIELDS_QUERY_PREFIX + fields.stream()
                .map(ProductFieldNames::wireName)
                .collect(Collectors.joining(FIELDS_SEPARATOR));
    }

    private static String encodePathSegment(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace(FORM_ENCODED_SPACE, PATH_ENCODED_SPACE);
    }
}
