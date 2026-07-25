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
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSortField;
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
            ProductResponse rawResponse = runtime.get(path(ApiPaths.PRODUCT_BY_EXTERNAL_ID, externalId),
                    ProductPaths.fieldsQuery(fields), ProductResponse.class);
            return Optional.ofNullable(rawResponse).map(ProductMapper::toDomain);
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
        ProductUpdateResponse rawResponse = runtime.patch(path(ApiPaths.PRODUCT_BY_EXTERNAL_ID, externalId),
                ProductRequestMapper.toUpdate(patch), ProductUpdateResponse.class);
        return ProductResultMapper.toUpdateResult(rawResponse);
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
        ProductBatchResponseInner[] rawResponse = runtime.patch(ApiPaths.PRODUCTS_BATCH_UPDATE, entries,
                ProductBatchResponseInner[].class);
        return rawResponse == null
                ? List.of()
                : Arrays.stream(rawResponse).map(ProductResultMapper::toBatchOutcome).toList();
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
        ProductResponse[] rawResponse = runtime.post(ApiPaths.PRODUCTS_SEARCH,
                ProductSearchMapper.toRequest(page), ProductResponse[].class);
        if (rawResponse == null || rawResponse.length == 0) {
            return new Page<>(List.of(), null);
        }
        List<Product> products = Arrays.stream(rawResponse).map(ProductMapper::toDomain).toList();
        if (products.size() < page.effectivePageSize()) {
            // A short page is the last one; reporting no cursor ends the walk without spending a request
            // to discover an empty page.
            return new Page<>(products, null);
        }
        requireUniqueSortForPaging(page.sortField());
        return new Page<>(products,
                ProductSearchMapper.cursorOf(products.get(products.size() - 1), page.sortField()).orElse(null));
    }

    /**
     * Refuse to page past the first page on a sort field that can repeat.
     *
     * <p>Erli's cursor is a <strong>strict</strong> bound on the sort field, so when several products
     * share the last row's value and did not fit on the page, the next page begins past all of them and
     * those products are silently never returned. That is a wrong answer presented as a complete one —
     * worse than an error — so the walk stops with an explanation instead. A single page is unaffected,
     * which keeps {@code search(...).limit(n)} on a non-unique sort perfectly usable.
     */
    private static void requireUniqueSortForPaging(ProductSortField sortField) {
        if (!sortField.isUniquePerProduct()) {
            throw new IllegalStateException(
                    "Cannot page beyond the first page sorted by " + sortField + ": Erli's cursor is a"
                            + " strict bound on the sort field, so products sharing the last row's value"
                            + " would be skipped. Sort by EXTERNAL_ID or MARKETPLACE_ID to walk the whole"
                            + " catalog, or keep the result within one page (see"
                            + " ProductSearchRequest.pageSize).");
        }
    }

    @Override
    public Discount startDiscount(ProductExternalId externalId, DiscountRequest request) {
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(request, "request");
        var rawResponse = runtime.post(path(ApiPaths.PRODUCT_DISCOUNT, externalId),
                ProductResultMapper.toCreateDiscount(request),
                io.github.mgrtomaszzurawski.erli.rest.model.Discount.class);
        return ProductResultMapper.toDiscount(rawResponse);
    }

    @Override
    public Optional<Discount> getDiscount(ProductExternalId externalId) {
        Objects.requireNonNull(externalId, "externalId");
        try {
            var rawResponse = runtime.get(path(ApiPaths.PRODUCT_DISCOUNT, externalId),
                    io.github.mgrtomaszzurawski.erli.rest.model.Discount.class);
            return Optional.ofNullable(rawResponse).map(ProductResultMapper::toDiscount);
        } catch (ErliNotFoundException absent) {
            // A product with no promotion is a normal state, not an error.
            return Optional.empty();
        }
    }
}
