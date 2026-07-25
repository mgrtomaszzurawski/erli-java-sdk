package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.model.Cursor;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.products.BatchUpdateOutcome;
import io.github.mgrtomaszzurawski.erli.domain.products.Discount;
import io.github.mgrtomaszzurawski.erli.domain.products.DiscountRequest;
import io.github.mgrtomaszzurawski.erli.domain.products.Product;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAccess;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDraft;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductPatch;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSearchRequest;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductUpdateResult;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.CursorPagination;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.Page;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductBatchResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductUpdateResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductsBatchUpdatePatchRequestInner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link ProductAccess} implementation: calls the {@code /products} endpoints through the shared
 * {@link HttpRuntime} and maps raw payloads to the domain records. Internal: never exported.
 */
public final class ProductAccessImpl implements ProductAccess {

    private final HttpRuntime runtime;

    public ProductAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    /**
     * A concrete request path from a template. Kept short and used inline at every call site so the
     * transport verb and its {@code ApiPaths} constant stay on one line — that pairing is what the
     * fleet's coverage auditor reads to prove an operation is really wired.
     */
    private static String path(String template, ProductExternalId externalId) {
        return ProductPaths.withExternalId(template, externalId);
    }

    @Override
    public Optional<Product> get(ProductExternalId externalId) {
        return get(externalId, Set.of());
    }

    @Override
    public Optional<Product> get(ProductExternalId externalId, Set<ProductField> fields) {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(fields, "fields");
        try {
            ProductResponse raw = runtime.get(path(ApiPaths.PRODUCT_BY_EXTERNAL_ID, externalId),
                    ProductPaths.fieldsQuery(fields), ProductResponse.class);
            return Optional.ofNullable(raw).map(ProductMapper::toDomain);
        } catch (ErliNotFoundException absent) {
            // "No such product" is an expected answer to a lookup, not a failure: the caller asked
            // whether it exists. Every other error still propagates.
            return Optional.empty();
        }
    }

    @Override
    public void create(ProductExternalId externalId, ProductDraft draft) {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(draft, "draft");
        // 202 Accepted with an empty body: there is nothing to decode, only to have succeeded.
        runtime.post(path(ApiPaths.PRODUCT_BY_EXTERNAL_ID, externalId),
                ProductRequestMapper.toCreate(draft), Void.class);
    }

    @Override
    public ProductUpdateResult update(ProductExternalId externalId, ProductPatch patch) {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(patch, "patch");
        ProductUpdateResponse raw = runtime.patch(path(ApiPaths.PRODUCT_BY_EXTERNAL_ID, externalId),
                ProductRequestMapper.toUpdate(patch), ProductUpdateResponse.class);
        return ProductResultMapper.toUpdateResult(raw);
    }

    @Override
    public List<BatchUpdateOutcome> updateAll(Map<ProductExternalId, ProductPatch> patches) {
        Objects.requireNonNull(patches, "patches");
        if (patches.isEmpty()) {
            // Nothing to change: an empty batch is a no-op, not a request worth spending on.
            return List.of();
        }
        List<ProductsBatchUpdatePatchRequestInner> entries = new ArrayList<>(patches.size());
        patches.forEach((externalId, patch) ->
                entries.add(ProductRequestMapper.toBatchEntry(externalId, patch)));
        ProductBatchResponseInner[] raw = runtime.patch(ApiPaths.PRODUCTS_BATCH_UPDATE, entries,
                ProductBatchResponseInner[].class);
        return raw == null
                ? List.of()
                : Arrays.stream(raw).map(ProductResultMapper::toBatchOutcome).toList();
    }

    @Override
    public Stream<Product> search(ProductSearchRequest request) {
        Objects.requireNonNull(request, "request");
        return CursorPagination.stream(after -> fetchPage(request, after));
    }

    /**
     * One page of a search. Erli answers with a bare array and no cursor, so the cursor for the next page
     * is derived from the last product's sort field (see {@link ProductSearchMapper}); a page shorter
     * than the requested size is the last one, and reporting no cursor stops the walk immediately rather
     * than spending a request to discover an empty page.
     */
    private Page<Product> fetchPage(ProductSearchRequest request, Cursor after) {
        ProductSearchRequest page = after == null ? request : request.after(after);
        ProductResponse[] raw = runtime.post(ApiPaths.PRODUCTS_SEARCH,
                ProductSearchMapper.toRequest(page), ProductResponse[].class);
        if (raw == null || raw.length == 0) {
            return new Page<>(List.of(), null);
        }
        List<Product> products = Arrays.stream(raw).map(ProductMapper::toDomain).toList();
        int requestedSize = page.pageSize().orElse(ProductSearchRequest.DEFAULT_PAGE_SIZE);
        Cursor next = products.size() < requestedSize
                ? null
                : ProductSearchMapper.cursorOf(products.get(products.size() - 1), page.sortField())
                        .orElse(null);
        return new Page<>(products, next);
    }

    @Override
    public Discount startDiscount(ProductExternalId externalId, DiscountRequest request) {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(request, "request");
        var raw = runtime.post(path(ApiPaths.PRODUCT_DISCOUNT, externalId),
                ProductResultMapper.toCreateDiscount(request),
                io.github.mgrtomaszzurawski.erli.rest.model.Discount.class);
        return ProductResultMapper.toDiscount(raw);
    }

    @Override
    public Optional<Discount> getDiscount(ProductExternalId externalId) {
        Objects.requireNonNull(externalId, "externalId");
        try {
            var raw = runtime.get(path(ApiPaths.PRODUCT_DISCOUNT, externalId),
                    io.github.mgrtomaszzurawski.erli.rest.model.Discount.class);
            return Optional.ofNullable(raw).map(ProductResultMapper::toDiscount);
        } catch (ErliNotFoundException absent) {
            // A product with no promotion is a normal state, not an error.
            return Optional.empty();
        }
    }
}
