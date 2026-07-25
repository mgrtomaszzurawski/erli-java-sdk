package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.util.Optional;
import java.util.Set;

/**
 * Access to the seller's product catalog. Reached via {@code client.products()}.
 *
 * <p>Every operation addresses a product by its seller-assigned {@link ProductExternalId} — Erli has no
 * separate create-then-use id, so the seller chooses the key and the same call creates or updates it.
 *
 * <pre>{@code
 * ProductAccess products = client.products();
 * Product product = products.get(ProductExternalId.of("sku-1")).orElseThrow();
 * }</pre>
 *
 * <p>Public surface — consumers import only this package.
 */
public interface ProductAccess {

    /**
     * Fetch one product ({@code GET /products/{externalId}}), with every field the marketplace holds.
     *
     * @param externalId the seller-assigned product id
     * @return the product, or empty when the catalog has no product under that id
     */
    Optional<Product> get(ProductExternalId externalId);

    /**
     * Fetch one product, asking the marketplace for only the given fields
     * ({@code GET /products/{externalId}?fields=…}).
     *
     * <p>A product payload is large; a projection keeps the response to what the caller will actually
     * read. Fields left out arrive empty, so select at least what you intend to use. Passing an empty set
     * asks for the whole product, exactly like {@link #get(ProductExternalId)}.
     *
     * @param externalId the seller-assigned product id
     * @param fields     the fields to return
     * @return the product, or empty when the catalog has no product under that id
     */
    Optional<Product> get(ProductExternalId externalId, Set<ProductField> fields);
}
