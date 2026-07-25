package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * A partial update to an existing product ({@code PATCH /products/{externalId}} and
 * {@code PATCH /products/batch-update}).
 *
 * <p>A patch distinguishes three intentions per field, and the API distinguishes them too:
 * <ul>
 *   <li><strong>leave alone</strong> — the field is simply not set on the {@link ProductContent};
 *       it is omitted from the request;
 *   <li><strong>set</strong> — the field carries a value;
 *   <li><strong>clear</strong> — named in {@link Builder#clear(ProductField...)}, sent as an explicit
 *       {@code null} so the marketplace removes the current value.
 * </ul>
 * Collapsing "leave alone" into "clear" is how a partial update silently wipes unrelated data, so the
 * two are kept apart all the way to the wire.
 *
 * <p>Only fields Erli declares nullable can be cleared; asking to clear any other is rejected here with
 * the field named, rather than producing a request the marketplace will refuse.
 *
 * <pre>{@code
 * ProductPatch patch = ProductPatch.builder()
 *         .content(ProductContent.builder().stock(5).build())   // set stock
 *         .clear(ProductField.MOBILE_PRICE)                     // remove the mobile price
 *         .build();
 * }</pre>
 */
public final class ProductPatch {

    /**
     * The fields Erli marks {@code nullable} on {@code ProductUpdate} — the only ones an update may
     * clear. Everything else can be changed but not removed.
     */
    private static final Set<ProductField> CLEARABLE_FIELDS = Collections.unmodifiableSet(EnumSet.of(
            ProductField.DESCRIPTION, ProductField.EAN, ProductField.SKU, ProductField.BASE_MARKET,
            ProductField.SOURCE_FULFILLMENT_PRODUCT_ID, ProductField.IMPORTANT_FEATURES,
            ProductField.EXTERNAL_ATTRIBUTES, ProductField.EXTERNAL_CATEGORIES,
            ProductField.EXTERNAL_VARIANT_GROUP, ProductField.EXTERNAL_RESPONSIBLE_PRODUCER,
            ProductField.EXTERNAL_RESPONSIBLE_PERSON, ProductField.FILES, ProductField.MOBILE_PRICE,
            ProductField.CATALOGUE_PRICE, ProductField.REFERENCE_PRICE_TYPE, ProductField.STATUS,
            ProductField.DELIVERY_PRICE_LIST, ProductField.WEIGHT, ProductField.OBLIGATORY_IDENTIFIER,
            ProductField.VOLUNTARY_IDENTIFIER, ProductField.RETURN_IDENTIFIER, ProductField.INVOICE_TYPE,
            ProductField.TAX_RATE, ProductField.BASKET_LIMIT, ProductField.ENERGY_LABEL,
            ProductField.INSTRUCTION_WITH_SAFETY_INFORMATION, ProductField.INFORMATION_CARD,
            ProductField.EXTERNAL_META_PRODUCT_ID, ProductField.EXTERNAL_PRODUCT_SETS,
            ProductField.PRODUCT_SETS, ProductField.PRODUCT_ATTACHMENTS,
            ProductField.AUTOMATIC_DISCOUNT_RULE_ID));

    private final ProductContent content;
    private final Set<ProductField> cleared;
    private final Optional<ProductExternalId> newExternalId;
    private final boolean overrideFrozen;

    private ProductPatch(Builder builder) {
        this.content = builder.content;
        // Copy, not a view: the builder stays usable after build() and must not reach back in.
        this.cleared = builder.cleared.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.cleared));
        this.newExternalId = builder.newExternalId;
        this.overrideFrozen = builder.overrideFrozen;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The fields this update sets. */
    public ProductContent content() {
        return content;
    }

    /** The fields this update removes. */
    public Set<ProductField> cleared() {
        return cleared;
    }

    /** The new external id, when the update also renames the product. */
    public Optional<ProductExternalId> newExternalId() {
        return newExternalId;
    }

    /** Whether the update is allowed to write fields the seller froze. */
    public boolean overrideFrozen() {
        return overrideFrozen;
    }

    /** The fields an update is permitted to clear. */
    public static Set<ProductField> clearableFields() {
        return CLEARABLE_FIELDS;
    }

    /** Builder for {@link ProductPatch}. */
    public static final class Builder {

        private ProductContent content = ProductContent.builder().build();
        private final Set<ProductField> cleared = EnumSet.noneOf(ProductField.class);
        private Optional<ProductExternalId> newExternalId = Optional.empty();
        private boolean overrideFrozen;

        private Builder() {
        }

        /** The fields to set. Replaces any content previously given to this builder. */
        public Builder content(ProductContent value) {
            this.content = value == null ? ProductContent.builder().build() : value;
            return this;
        }

        /**
         * Remove the given fields, sending an explicit {@code null} for each.
         *
         * @param fields the fields to clear
         * @throws IllegalArgumentException if a field is one Erli does not allow an update to clear
         */
        public Builder clear(ProductField... fields) {
            for (ProductField field : fields) {
                if (!CLEARABLE_FIELDS.contains(field)) {
                    throw new IllegalArgumentException(
                            "Erli does not allow an update to clear '" + field + "'; it can be changed but "
                                    + "not removed. Clearable fields: " + CLEARABLE_FIELDS);
                }
                cleared.add(field);
            }
            return this;
        }

        /** Rename the product, changing the external id every later call must use. */
        public Builder newExternalId(ProductExternalId value) {
            this.newExternalId = Optional.ofNullable(value);
            return this;
        }

        /** Allow this update to write fields the seller pinned with {@link FrozenFields}. */
        public Builder overrideFrozen(boolean value) {
            this.overrideFrozen = value;
            return this;
        }

        public ProductPatch build() {
            return new ProductPatch(this);
        }
    }
}
