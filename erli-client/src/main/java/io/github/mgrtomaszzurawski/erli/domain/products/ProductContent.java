package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.TaxRate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The authorable content of a product — every field a seller may write, and nothing the marketplace
 * derives. It is the payload shared by {@link ProductDraft} (create) and {@link ProductPatch} (update),
 * which differ in their rules, not in their fields.
 *
 * <p>Each field is an {@link Optional} so that "not supplied" stays distinct from "supplied as empty".
 * On an update that distinction is the difference between leaving a field alone and changing it; a field
 * left unset here is omitted from the request entirely.
 *
 * <p>Build one with {@link #builder()}; instances are immutable and reusable across calls.
 */
public final class ProductContent {

    private final Optional<String> name;
    private final Optional<ProductDescription> description;
    private final Optional<String> ean;
    private final Optional<String> sku;
    private final Optional<BaseMarket> baseMarket;
    private final Optional<List<ExternalReference>> externalReferences;
    private final Optional<Integer> sourceFulfillmentProductId;
    private final Optional<List<String>> importantFeatures;
    private final Optional<List<ExternalAttribute>> externalAttributes;
    private final Optional<List<ExternalCategory>> externalCategories;
    private final Optional<ExternalVariantGroup> externalVariantGroup;
    private final Optional<List<ExternalResponsibleEntity>> externalResponsibleProducer;
    private final Optional<List<ExternalResponsibleEntity>> externalResponsiblePerson;
    private final Optional<List<ProductImage>> images;
    private final Optional<List<ProductFile>> files;
    private final Optional<Money> price;
    private final Optional<Money> mobilePrice;
    private final Optional<Money> cataloguePrice;
    private final Optional<ReferencePriceType> referencePriceType;
    private final Optional<Integer> stock;
    private final Optional<ProductStatus> status;
    private final Optional<Boolean> archived;
    private final Optional<DispatchTime> dispatchTime;
    private final Optional<String> deliveryPriceList;
    private final Optional<BigDecimal> weight;
    private final Optional<String> obligatoryIdentifier;
    private final Optional<String> voluntaryIdentifier;
    private final Optional<String> returnIdentifier;
    private final Optional<InvoiceType> invoiceType;
    private final Optional<TaxRate> taxRate;
    private final Optional<Integer> basketLimit;
    private final Optional<String> energyLabel;
    private final Optional<String> instructionWithSafetyInformation;
    private final Optional<String> informationCard;
    private final Optional<Integer> producerId;
    private final Optional<Integer> responsiblePersonId;
    private final Optional<String> externalMetaProductId;
    private final Optional<ExternalProductSet> externalProductSets;
    private final Optional<ProductSet> productSets;
    private final Optional<List<ProductAttachment>> productAttachments;
    private final Optional<Integer> automaticDiscountRuleId;
    private final Optional<Packaging> packaging;
    private final Optional<FrozenFields> frozen;

    private ProductContent(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.ean = builder.ean;
        this.sku = builder.sku;
        this.baseMarket = builder.baseMarket;
        this.externalReferences = builder.externalReferences;
        this.sourceFulfillmentProductId = builder.sourceFulfillmentProductId;
        this.importantFeatures = builder.importantFeatures;
        this.externalAttributes = builder.externalAttributes;
        this.externalCategories = builder.externalCategories;
        this.externalVariantGroup = builder.externalVariantGroup;
        this.externalResponsibleProducer = builder.externalResponsibleProducer;
        this.externalResponsiblePerson = builder.externalResponsiblePerson;
        this.images = builder.images;
        this.files = builder.files;
        this.price = builder.price;
        this.mobilePrice = builder.mobilePrice;
        this.cataloguePrice = builder.cataloguePrice;
        this.referencePriceType = builder.referencePriceType;
        this.stock = builder.stock;
        this.status = builder.status;
        this.archived = builder.archived;
        this.dispatchTime = builder.dispatchTime;
        this.deliveryPriceList = builder.deliveryPriceList;
        this.weight = builder.weight;
        this.obligatoryIdentifier = builder.obligatoryIdentifier;
        this.voluntaryIdentifier = builder.voluntaryIdentifier;
        this.returnIdentifier = builder.returnIdentifier;
        this.invoiceType = builder.invoiceType;
        this.taxRate = builder.taxRate;
        this.basketLimit = builder.basketLimit;
        this.energyLabel = builder.energyLabel;
        this.instructionWithSafetyInformation = builder.instructionWithSafetyInformation;
        this.informationCard = builder.informationCard;
        this.producerId = builder.producerId;
        this.responsiblePersonId = builder.responsiblePersonId;
        this.externalMetaProductId = builder.externalMetaProductId;
        this.externalProductSets = builder.externalProductSets;
        this.productSets = builder.productSets;
        this.productAttachments = builder.productAttachments;
        this.automaticDiscountRuleId = builder.automaticDiscountRuleId;
        this.packaging = builder.packaging;
        this.frozen = builder.frozen;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The product name shown to buyers. */
    public Optional<String> name() {
        return name;
    }

    /** The structured description. */
    public Optional<ProductDescription> description() {
        return description;
    }

    /** The EAN barcode. */
    public Optional<String> ean() {
        return ean;
    }

    /** The seller's stock-keeping unit. */
    public Optional<String> sku() {
        return sku;
    }

    /** The market the product's own content is authored for. */
    public Optional<BaseMarket> baseMarket() {
        return baseMarket;
    }

    /** Pointers to this product on other sites. */
    public Optional<List<ExternalReference>> externalReferences() {
        return externalReferences;
    }

    /** The fulfilment source product id. */
    public Optional<Integer> sourceFulfillmentProductId() {
        return sourceFulfillmentProductId;
    }

    /** Bullet points highlighted on the offer page. */
    public Optional<List<String>> importantFeatures() {
        return importantFeatures;
    }

    /** Seller-supplied attributes, before catalog matching. */
    public Optional<List<ExternalAttribute>> externalAttributes() {
        return externalAttributes;
    }

    /** Seller-supplied category paths, before matching. */
    public Optional<List<ExternalCategory>> externalCategories() {
        return externalCategories;
    }

    /** The variant grouping the seller declares. */
    public Optional<ExternalVariantGroup> externalVariantGroup() {
        return externalVariantGroup;
    }

    /** GPSR producer references. */
    public Optional<List<ExternalResponsibleEntity>> externalResponsibleProducer() {
        return externalResponsibleProducer;
    }

    /** GPSR responsible-person references. */
    public Optional<List<ExternalResponsibleEntity>> externalResponsiblePerson() {
        return externalResponsiblePerson;
    }

    /** The product images; the first is the cover. */
    public Optional<List<ProductImage>> images() {
        return images;
    }

    /** Files attached to the product. */
    public Optional<List<ProductFile>> files() {
        return files;
    }

    /** The selling price. */
    public Optional<Money> price() {
        return price;
    }

    /** A lower price shown in the mobile app. */
    public Optional<Money> mobilePrice() {
        return mobilePrice;
    }

    /** The seller's catalogue ("list") price. */
    public Optional<Money> cataloguePrice() {
        return cataloguePrice;
    }

    /** Which price the marketplace strikes through. */
    public Optional<ReferencePriceType> referencePriceType() {
        return referencePriceType;
    }

    /** Units available. */
    public Optional<Integer> stock() {
        return stock;
    }

    /** Whether the product is offered for sale. */
    public Optional<ProductStatus> status() {
        return status;
    }

    /** Whether the product is archived. */
    public Optional<Boolean> archived() {
        return archived;
    }

    /** How long the seller takes to hand the product to the carrier. */
    public Optional<DispatchTime> dispatchTime() {
        return dispatchTime;
    }

    /** The name of the delivery price list that applies. */
    public Optional<String> deliveryPriceList() {
        return deliveryPriceList;
    }

    /** The product weight in grams, as Erli states it. */
    public Optional<BigDecimal> weight() {
        return weight;
    }

    /** The legally required product identifier. */
    public Optional<String> obligatoryIdentifier() {
        return obligatoryIdentifier;
    }

    /** An optional additional identifier. */
    public Optional<String> voluntaryIdentifier() {
        return voluntaryIdentifier;
    }

    /** The identifier used on returns. */
    public Optional<String> returnIdentifier() {
        return returnIdentifier;
    }

    /** The kind of invoice the seller issues. */
    public Optional<InvoiceType> invoiceType() {
        return invoiceType;
    }

    /** The VAT rate applied. */
    public Optional<TaxRate> taxRate() {
        return taxRate;
    }

    /** The maximum units per order. */
    public Optional<Integer> basketLimit() {
        return basketLimit;
    }

    /** The EU energy-label document reference. */
    public Optional<String> energyLabel() {
        return energyLabel;
    }

    /** The safety-instructions document reference. */
    public Optional<String> instructionWithSafetyInformation() {
        return instructionWithSafetyInformation;
    }

    /** The product information card reference. */
    public Optional<String> informationCard() {
        return informationCard;
    }

    /** The producer dictionary id. */
    public Optional<Integer> producerId() {
        return producerId;
    }

    /** The responsible-person dictionary id. */
    public Optional<Integer> responsiblePersonId() {
        return responsiblePersonId;
    }

    /** The seller's meta-product id for bundling. */
    public Optional<String> externalMetaProductId() {
        return externalMetaProductId;
    }

    /** The bundle declared in the seller's own ids. */
    public Optional<ExternalProductSet> externalProductSets() {
        return externalProductSets;
    }

    /** The bundle expressed in marketplace meta-product ids. */
    public Optional<ProductSet> productSets() {
        return productSets;
    }

    /** Documents attached to the product. */
    public Optional<List<ProductAttachment>> productAttachments() {
        return productAttachments;
    }

    /** The automatic-discount rule that applies. */
    public Optional<Integer> automaticDiscountRuleId() {
        return automaticDiscountRuleId;
    }

    /** Packaging characteristics. */
    public Optional<Packaging> packaging() {
        return packaging;
    }

    /** The fields to pin against integration overwrites. */
    public Optional<FrozenFields> frozen() {
        return frozen;
    }

    /** Builder for {@link ProductContent}. Every field is optional here; the rules live in
     * {@link ProductDraft} and {@link ProductPatch}. */
    public static final class Builder {

        private Optional<String> name = Optional.empty();
        private Optional<ProductDescription> description = Optional.empty();
        private Optional<String> ean = Optional.empty();
        private Optional<String> sku = Optional.empty();
        private Optional<BaseMarket> baseMarket = Optional.empty();
        private Optional<List<ExternalReference>> externalReferences = Optional.empty();
        private Optional<Integer> sourceFulfillmentProductId = Optional.empty();
        private Optional<List<String>> importantFeatures = Optional.empty();
        private Optional<List<ExternalAttribute>> externalAttributes = Optional.empty();
        private Optional<List<ExternalCategory>> externalCategories = Optional.empty();
        private Optional<ExternalVariantGroup> externalVariantGroup = Optional.empty();
        private Optional<List<ExternalResponsibleEntity>> externalResponsibleProducer = Optional.empty();
        private Optional<List<ExternalResponsibleEntity>> externalResponsiblePerson = Optional.empty();
        private Optional<List<ProductImage>> images = Optional.empty();
        private Optional<List<ProductFile>> files = Optional.empty();
        private Optional<Money> price = Optional.empty();
        private Optional<Money> mobilePrice = Optional.empty();
        private Optional<Money> cataloguePrice = Optional.empty();
        private Optional<ReferencePriceType> referencePriceType = Optional.empty();
        private Optional<Integer> stock = Optional.empty();
        private Optional<ProductStatus> status = Optional.empty();
        private Optional<Boolean> archived = Optional.empty();
        private Optional<DispatchTime> dispatchTime = Optional.empty();
        private Optional<String> deliveryPriceList = Optional.empty();
        private Optional<BigDecimal> weight = Optional.empty();
        private Optional<String> obligatoryIdentifier = Optional.empty();
        private Optional<String> voluntaryIdentifier = Optional.empty();
        private Optional<String> returnIdentifier = Optional.empty();
        private Optional<InvoiceType> invoiceType = Optional.empty();
        private Optional<TaxRate> taxRate = Optional.empty();
        private Optional<Integer> basketLimit = Optional.empty();
        private Optional<String> energyLabel = Optional.empty();
        private Optional<String> instructionWithSafetyInformation = Optional.empty();
        private Optional<String> informationCard = Optional.empty();
        private Optional<Integer> producerId = Optional.empty();
        private Optional<Integer> responsiblePersonId = Optional.empty();
        private Optional<String> externalMetaProductId = Optional.empty();
        private Optional<ExternalProductSet> externalProductSets = Optional.empty();
        private Optional<ProductSet> productSets = Optional.empty();
        private Optional<List<ProductAttachment>> productAttachments = Optional.empty();
        private Optional<Integer> automaticDiscountRuleId = Optional.empty();
        private Optional<Packaging> packaging = Optional.empty();
        private Optional<FrozenFields> frozen = Optional.empty();

        private Builder() {
        }

    /** Set the product name shown to buyers. */
        public Builder name(String value) {
            this.name = Optional.ofNullable(value);
            return this;
        }

    /** Set the structured description. */
        public Builder description(ProductDescription value) {
            this.description = Optional.ofNullable(value);
            return this;
        }

    /** Set the EAN barcode. */
        public Builder ean(String value) {
            this.ean = Optional.ofNullable(value);
            return this;
        }

    /** Set the seller's stock-keeping unit. */
        public Builder sku(String value) {
            this.sku = Optional.ofNullable(value);
            return this;
        }

    /** Set the market the product's own content is authored for. */
        public Builder baseMarket(BaseMarket value) {
            this.baseMarket = Optional.ofNullable(value);
            return this;
        }

    /** Set pointers to this product on other sites. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder externalReferences(List<ExternalReference> value) {
            this.externalReferences = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set the fulfilment source product id. */
        public Builder sourceFulfillmentProductId(Integer value) {
            this.sourceFulfillmentProductId = Optional.ofNullable(value);
            return this;
        }

    /** Set bullet points highlighted on the offer page. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder importantFeatures(List<String> value) {
            this.importantFeatures = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set seller-supplied attributes, before catalog matching. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder externalAttributes(List<ExternalAttribute> value) {
            this.externalAttributes = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set seller-supplied category paths, before matching. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder externalCategories(List<ExternalCategory> value) {
            this.externalCategories = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set the variant grouping the seller declares. */
        public Builder externalVariantGroup(ExternalVariantGroup value) {
            this.externalVariantGroup = Optional.ofNullable(value);
            return this;
        }

    /** Set GPSR producer references. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder externalResponsibleProducer(List<ExternalResponsibleEntity> value) {
            this.externalResponsibleProducer = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set GPSR responsible-person references. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder externalResponsiblePerson(List<ExternalResponsibleEntity> value) {
            this.externalResponsiblePerson = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set the product images; the first is the cover. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder images(List<ProductImage> value) {
            this.images = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set files attached to the product. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder files(List<ProductFile> value) {
            this.files = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set the selling price. */
        public Builder price(Money value) {
            this.price = Optional.ofNullable(value);
            return this;
        }

    /** Set a lower price shown in the mobile app. */
        public Builder mobilePrice(Money value) {
            this.mobilePrice = Optional.ofNullable(value);
            return this;
        }

    /** Set the seller's catalogue ("list") price. */
        public Builder cataloguePrice(Money value) {
            this.cataloguePrice = Optional.ofNullable(value);
            return this;
        }

    /** Set which price the marketplace strikes through. */
        public Builder referencePriceType(ReferencePriceType value) {
            this.referencePriceType = Optional.ofNullable(value);
            return this;
        }

    /** Set units available. */
        public Builder stock(Integer value) {
            this.stock = Optional.ofNullable(value);
            return this;
        }

    /** Set whether the product is offered for sale. */
        public Builder status(ProductStatus value) {
            this.status = Optional.ofNullable(value);
            return this;
        }

    /** Set whether the product is archived. */
        public Builder archived(Boolean value) {
            this.archived = Optional.ofNullable(value);
            return this;
        }

    /** Set how long the seller takes to hand the product to the carrier. */
        public Builder dispatchTime(DispatchTime value) {
            this.dispatchTime = Optional.ofNullable(value);
            return this;
        }

    /** Set the name of the delivery price list that applies. */
        public Builder deliveryPriceList(String value) {
            this.deliveryPriceList = Optional.ofNullable(value);
            return this;
        }

    /** Set the product weight in grams, as Erli states it. */
        public Builder weight(BigDecimal value) {
            this.weight = Optional.ofNullable(value);
            return this;
        }

    /** Set the legally required product identifier. */
        public Builder obligatoryIdentifier(String value) {
            this.obligatoryIdentifier = Optional.ofNullable(value);
            return this;
        }

    /** Set an optional additional identifier. */
        public Builder voluntaryIdentifier(String value) {
            this.voluntaryIdentifier = Optional.ofNullable(value);
            return this;
        }

    /** Set the identifier used on returns. */
        public Builder returnIdentifier(String value) {
            this.returnIdentifier = Optional.ofNullable(value);
            return this;
        }

    /** Set the kind of invoice the seller issues. */
        public Builder invoiceType(InvoiceType value) {
            this.invoiceType = Optional.ofNullable(value);
            return this;
        }

    /** Set the VAT rate applied. */
        public Builder taxRate(TaxRate value) {
            this.taxRate = Optional.ofNullable(value);
            return this;
        }

    /** Set the maximum units per order. */
        public Builder basketLimit(Integer value) {
            this.basketLimit = Optional.ofNullable(value);
            return this;
        }

    /** Set the EU energy-label document reference. */
        public Builder energyLabel(String value) {
            this.energyLabel = Optional.ofNullable(value);
            return this;
        }

    /** Set the safety-instructions document reference. */
        public Builder instructionWithSafetyInformation(String value) {
            this.instructionWithSafetyInformation = Optional.ofNullable(value);
            return this;
        }

    /** Set the product information card reference. */
        public Builder informationCard(String value) {
            this.informationCard = Optional.ofNullable(value);
            return this;
        }

    /** Set the producer dictionary id. */
        public Builder producerId(Integer value) {
            this.producerId = Optional.ofNullable(value);
            return this;
        }

    /** Set the responsible-person dictionary id. */
        public Builder responsiblePersonId(Integer value) {
            this.responsiblePersonId = Optional.ofNullable(value);
            return this;
        }

    /** Set the seller's meta-product id for bundling. */
        public Builder externalMetaProductId(String value) {
            this.externalMetaProductId = Optional.ofNullable(value);
            return this;
        }

    /** Set the bundle declared in the seller's own ids. */
        public Builder externalProductSets(ExternalProductSet value) {
            this.externalProductSets = Optional.ofNullable(value);
            return this;
        }

    /** Set the bundle expressed in marketplace meta-product ids. */
        public Builder productSets(ProductSet value) {
            this.productSets = Optional.ofNullable(value);
            return this;
        }

    /** Set documents attached to the product. A null clears the setting; an empty list is a real value meaning "no entries". */
        public Builder productAttachments(List<ProductAttachment> value) {
            this.productAttachments = Optional.ofNullable(value).map(List::copyOf);
            return this;
        }

    /** Set the automatic-discount rule that applies. */
        public Builder automaticDiscountRuleId(Integer value) {
            this.automaticDiscountRuleId = Optional.ofNullable(value);
            return this;
        }

    /** Set packaging characteristics. */
        public Builder packaging(Packaging value) {
            this.packaging = Optional.ofNullable(value);
            return this;
        }

    /** Set the fields to pin against integration overwrites. */
        public Builder frozen(FrozenFields value) {
            this.frozen = Optional.ofNullable(value);
            return this;
        }

        public ProductContent build() {
            return new ProductContent(this);
        }
    }
}
