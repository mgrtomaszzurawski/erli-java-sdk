package io.github.mgrtomaszzurawski.erli.domain.hooks;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.util.List;
import java.util.Objects;

/**
 * Which products (and optionally which of their fields) a {@code productsNeedSync} test-fire should
 * name. An empty {@link #fields()} means the whole product needs re-synchronising.
 *
 * @param productIds the products, between one and {@value #MAX_PRODUCT_IDS} of them
 * @param fields     the field names that need syncing, or empty for the whole product
 */
public record ProductSyncNotification(List<ProductExternalId> productIds, List<String> fields) {

    /** The API's limit on how many products one call may name. */
    public static final int MAX_PRODUCT_IDS = 1000;

    private static final int MIN_PRODUCT_IDS = 1;

    public ProductSyncNotification {
        Objects.requireNonNull(productIds, "productIds");
        Objects.requireNonNull(fields, "fields");
        if (productIds.size() < MIN_PRODUCT_IDS || productIds.size() > MAX_PRODUCT_IDS) {
            throw new IllegalArgumentException(
                    "productIds must hold between " + MIN_PRODUCT_IDS + " and " + MAX_PRODUCT_IDS + " entries");
        }
        productIds = List.copyOf(productIds);
        fields = List.copyOf(fields);
    }

    /** Ask the shop to re-sync whole products. */
    public static ProductSyncNotification ofProducts(List<ProductExternalId> productIds) {
        return new ProductSyncNotification(productIds, List.of());
    }

    /** Ask the shop to re-sync only the named fields of the given products. */
    public static ProductSyncNotification ofFields(List<ProductExternalId> productIds, List<String> fields) {
        return new ProductSyncNotification(productIds, fields);
    }
}
