package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.products.Product;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAccess;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponse;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ProductAccess} implementation: calls the {@code /products} endpoints through the shared
 * {@link HttpRuntime} and maps raw payloads to the domain records. Internal: never exported.
 */
public final class ProductAccessImpl implements ProductAccess {

    private final HttpRuntime runtime;

    public ProductAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public Optional<Product> get(ProductExternalId externalId) {
        return get(externalId, Set.of());
    }

    @Override
    public Optional<Product> get(ProductExternalId externalId, Set<ProductField> fields) {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(fields, "fields");
        String path = ProductPaths.withFields(
                ProductPaths.withExternalId(ApiPaths.PRODUCT_BY_EXTERNAL_ID, externalId), fields);
        try {
            ProductResponse raw = runtime.get(path, ProductResponse.class);
            return Optional.ofNullable(raw).map(ProductMapper::toDomain);
        } catch (ErliNotFoundException absent) {
            // "No such product" is an expected answer to a lookup, not a failure: the caller asked
            // whether it exists. Every other error still propagates.
            return Optional.empty();
        }
    }
}
