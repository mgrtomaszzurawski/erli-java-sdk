package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.ArrayList;
import java.util.List;

/**
 * A new product to publish ({@code POST /products/{externalId}}).
 *
 * <p>Erli requires five fields on create — name, price, stock, dispatch time and at least one image —
 * and they are checked here rather than at the marketplace, so a missing one is a local
 * {@link IllegalStateException} naming the field instead of a round-trip and a 400.
 *
 * <pre>{@code
 * ProductDraft draft = ProductDraft.of(ProductContent.builder()
 *         .name("Kurtka zimowa")
 *         .price(Money.ofPln("100.00"))
 *         .stock(10)
 *         .dispatchTime(DispatchTime.ofDays(1))
 *         .images(List.of(ProductImage.of("https://example.com/cover.jpg")))
 *         .build());
 *
 * client.products().create(ProductExternalId.of("sku-1"), draft);
 * }</pre>
 */
public final class ProductDraft {

    private final ProductContent content;

    private ProductDraft(ProductContent content) {
        this.content = content;
    }

    /**
     * Wrap authored content as a create request, checking the fields Erli requires.
     *
     * @param content the product's authored content
     * @return the draft
     * @throws IllegalStateException if a field required on create is missing
     */
    public static ProductDraft of(ProductContent content) {
        List<String> missing = new ArrayList<>();
        if (content.name().isEmpty()) {
            missing.add("name");
        }
        if (content.price().isEmpty()) {
            missing.add("price");
        }
        if (content.stock().isEmpty()) {
            missing.add("stock");
        }
        if (content.dispatchTime().isEmpty()) {
            missing.add("dispatchTime");
        }
        if (content.images().map(List::isEmpty).orElse(true)) {
            missing.add("images");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "A new product is missing the field(s) Erli requires on create: " + String.join(", ", missing));
        }
        return new ProductDraft(content);
    }

    /** The authored content of the new product. */
    public ProductContent content() {
        return content;
    }
}
