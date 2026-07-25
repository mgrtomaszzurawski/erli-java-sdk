package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Access to the seller's product catalog. Reached via {@code client.products()}.
 *
 * <p>Every operation addresses a product by its seller-assigned {@link ProductExternalId} — Erli has no
 * separate create-then-use id, so the seller chooses the key up front and keeps using it.
 *
 * <pre>{@code
 * ProductAccess products = client.products();
 *
 * products.create(ProductExternalId.of("sku-1"), ProductDraft.of(ProductContent.builder()
 *         .name("Kurtka zimowa")
 *         .price(Money.ofPln("100.00"))
 *         .stock(10)
 *         .dispatchTime(DispatchTime.ofDays(1))
 *         .images(List.of(ProductImage.of("https://example.com/cover.jpg")))
 *         .build()));
 *
 * Product product = products.get(ProductExternalId.of("sku-1")).orElseThrow();
 *
 * try (Stream<Product> active = products.search(ProductSearchRequest.builder()
 *         .filter(ProductFilter.equalTo(ProductFilterField.STATUS, "active"))
 *         .build())) {
 *     active.limit(100).forEach(this::index);
 * }
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
     * read. Fields left out arrive empty, so select at least what you intend to use. An empty set asks
     * for the whole product, exactly like {@link #get(ProductExternalId)}.
     *
     * <p>The selection is widened with the handful of fields a {@link Product} cannot be built without
     * ({@code externalId}, {@code name}, {@code price}, {@code slug}, {@code created}, …) — otherwise a
     * projection that omitted one would return a payload the SDK could not map. The costly parts of a
     * product (description, attributes, translations, images) stay excluded unless selected.
     *
     * @param externalId the seller-assigned product id
     * @param fields     the fields to return
     * @return the product, or empty when the catalog has no product under that id
     */
    Optional<Product> get(ProductExternalId externalId, Set<ProductField> fields);

    /**
     * Publish a new product ({@code POST /products/{externalId}}).
     *
     * <p>The marketplace accepts the product for asynchronous processing and answers {@code 202} with no
     * body, so a successful return means "accepted", not "already visible". Read it back with
     * {@link #get(ProductExternalId)} to observe the marketplace-resolved fields.
     *
     * @param externalId the id to publish the product under
     * @param draft      the product to create
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException if a product already
     *                                                                            exists under that id
     */
    void create(ProductExternalId externalId, ProductDraft draft);

    /**
     * Change an existing product ({@code PATCH /products/{externalId}}).
     *
     * @param externalId the product to change
     * @param patch      the fields to set and clear
     * @return what the marketplace reports as changed
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException if no such product exists
     */
    ProductUpdateResult update(ProductExternalId externalId, ProductPatch patch);

    /**
     * Change many products in one call ({@code PATCH /products/batch-update}).
     *
     * <p>The call as a whole can succeed while individual products are rejected, so the outcome is
     * returned per product rather than thrown — inspect {@link BatchUpdateOutcome#isAccepted()} on each.
     *
     * @param patches the update to apply to each product, keyed by product id
     * @return one outcome per entry, in the order the marketplace returned them
     */
    List<BatchUpdateOutcome> updateAll(Map<ProductExternalId, ProductPatch> patches);

    /**
     * Search the catalog ({@code POST /products/_search}), streaming results across pages.
     *
     * <p>The stream is lazy: pages are fetched as it is consumed, so a {@code limit} or a short-circuit
     * costs only the pages actually needed. Erli returns a bare array with no cursor in the body, so the
     * SDK derives the next page's cursor from the sort field of the last item — which is why
     * {@link ProductSearchRequest.Builder#sortBy} must be set before the walk, not during it.
     *
     * <p>The stream holds no resources of its own; closing it is optional but harmless.
     *
     * @param request what to match and how to order it
     * @return a lazy stream over every matching product
     * @throws IllegalStateException when the stream is asked to continue past the first page while
     *                               sorted by a field that can repeat. Erli's cursor is a strict bound,
     *                               so products sharing the last row's value would be skipped; only
     *                               {@link ProductSortField#EXTERNAL_ID} and
     *                               {@link ProductSortField#MARKETPLACE_ID} are unique per product. A
     *                               single page is always delivered, whatever the sort.
     */
    Stream<Product> search(ProductSearchRequest request);

    /**
     * Start a timed promotion on a product ({@code POST /products/{externalId}/discount}).
     *
     * @param externalId the product to discount
     * @param request    the promotion to start
     * @return the promotion as the marketplace recorded it
     */
    Discount startDiscount(ProductExternalId externalId, DiscountRequest request);

    /**
     * Read the timed promotion recorded against a product
     * ({@code GET /products/{externalId}/discount}).
     *
     * @param externalId the product to inspect
     * @return the promotion, or empty when the product has none
     */
    Optional<Discount> getDiscount(ProductExternalId externalId);
}
