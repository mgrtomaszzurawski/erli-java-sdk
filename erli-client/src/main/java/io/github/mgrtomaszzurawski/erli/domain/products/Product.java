package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.core.model.TaxRate;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * A product in the seller's catalog, as returned by {@code GET /products/{externalId}} and
 * {@code POST /products/_search}.
 *
 * <p>The payload mixes three kinds of field, and this record keeps them distinguishable:
 * <ul>
 *   <li><strong>seller-authored</strong> — name, prices, stock, images, …: what you send on create/update;
 *   <li><strong>marketplace-resolved</strong> — {@link #attributes()}, {@link #categories()},
 *       {@link #translations()}, {@link #slug()}: the marketplace's interpretation of the
 *       {@code external*} content you supplied, never sent back;
 *   <li><strong>marketplace-managed</strong> — {@link #marketplaceId()}, {@link #created()},
 *       {@link #buyableProblems()}: lifecycle state.
 * </ul>
 *
 * <p>Prices arrive as integer grosze on the wire and are exposed as {@link Money} in PLN, so callers
 * never divide by 100 by hand. Fields the marketplace may omit are {@link Optional}; required fields
 * are plain, and a missing one is a mapping error rather than a silent {@code null}.
 *
 * @param externalId                       the seller-assigned identifier, the key every product operation addresses
 * @param marketplaceId                    the marketplace's own numeric id
 * @param name                             the product name shown to buyers
 * @param slug                             the URL slug the marketplace derived from the name
 * @param status                           whether the product is offered for sale
 * @param stock                            units available
 * @param price                            the selling price
 * @param mobilePrice                      a lower price shown in the mobile app, when set
 * @param cataloguePrice                   the seller's catalogue ("list") price, when set
 * @param referencePriceType               which price the marketplace strikes through as the reference price
 * @param description                      the structured description, when the product has one
 * @param externalDescription              a raw description supplied by an integration, when present
 * @param externalDescriptionHash          the marketplace's change-detection hash of {@code externalDescription}
 * @param ean                              the EAN barcode, when set
 * @param sku                              the seller's stock-keeping unit, when set
 * @param baseMarket                       the market the product's own content is authored for
 * @param markets                          the market the product is published to. The spec defaults
 *                                         this to {@code pl}, so an omitted value reads back as
 *                                         {@link Market#PL}; absent means the marketplace named a
 *                                         market this SDK version cannot (see {@link Market})
 * @param importantFeatures                bullet points highlighted on the offer page (defensively copied)
 * @param images                           the product images, first one is the cover (defensively copied)
 * @param files                            files attached to the product (defensively copied)
 * @param externalReferences               pointers to this product on other sites (defensively copied)
 * @param externalAttributes               seller-supplied attributes, before catalog matching (defensively copied)
 * @param externalCategories               seller-supplied category paths, before matching (defensively copied)
 * @param externalVariantGroup             the variant grouping the seller declared, when set
 * @param externalResponsibleProducer      GPSR producer references the seller declared (defensively copied)
 * @param externalResponsiblePerson        GPSR responsible-person references the seller declared (defensively copied)
 * @param externalMetaProductId            the seller's meta-product id for bundling, when set
 * @param externalProductSets              the bundle the seller declared in their own ids, when set
 * @param attributes                       attributes as resolved by the marketplace catalog (defensively copied)
 * @param categories                       category paths as resolved by the marketplace; each entry is a
 *                                         root-to-leaf path (defensively copied)
 * @param productSets                      the bundle as resolved to marketplace meta-products, when set
 * @param productAttachments               documents attached to the product (defensively copied)
 * @param translations                     marketplace-produced translations, when any exist
 * @param dispatchTime                     how long the seller takes to hand the product to the carrier
 * @param deliveryPriceList                the name of the delivery price list that applies, when set
 * @param weight                           the product weight in grams, as Erli states it, when set
 * @param packaging                        packaging characteristics, when set
 * @param basketLimit                      the maximum units per order, when the seller caps it
 * @param invoiceType                      the kind of invoice the seller issues
 * @param taxRate                          the VAT rate applied
 * @param obligatoryIdentifier             the legally required product identifier, when set
 * @param voluntaryIdentifier              an optional additional identifier, when set
 * @param returnIdentifier                 the identifier used on returns, when set
 * @param energyLabel                      the EU energy-label document reference, when set
 * @param instructionWithSafetyInformation the safety-instructions document reference, when set
 * @param informationCard                  the product information card reference, when set
 * @param producerId                       the resolved producer dictionary id, when set
 * @param producerIds                      all resolved producer dictionary ids (defensively copied)
 * @param responsiblePersonId              the resolved responsible-person dictionary id, when set
 * @param responsiblePersonIds             all resolved responsible-person dictionary ids (defensively copied)
 * @param sourceFulfillmentProductId       the fulfilment source product id, when the product is fulfilled by Erli
 * @param automaticDiscountRuleId          the automatic-discount rule that applies, when set
 * @param archived                         whether the product is archived
 * @param archivedAt                       when it was archived, when it has been
 * @param frozen                           the fields pinned against integration overwrites
 * @param buyableProblems                  reasons the marketplace currently will not sell the product
 *                                         (defensively copied); an empty list means it is buyable
 * @param created                          when the product was created
 * @param updated                          when the product was last changed, when known
 */
public record Product(
        ProductExternalId externalId,
        long marketplaceId,
        String name,
        String slug,
        ProductStatus status,
        int stock,
        Money price,
        Optional<Money> mobilePrice,
        Optional<Money> cataloguePrice,
        Optional<ReferencePriceType> referencePriceType,
        Optional<ProductDescription> description,
        Optional<String> externalDescription,
        Optional<String> externalDescriptionHash,
        Optional<String> ean,
        Optional<String> sku,
        Optional<BaseMarket> baseMarket,
        Optional<Market> markets,
        List<String> importantFeatures,
        List<ProductImage> images,
        List<ProductFile> files,
        List<ExternalReference> externalReferences,
        List<ExternalAttribute> externalAttributes,
        List<ExternalCategory> externalCategories,
        Optional<ExternalVariantGroup> externalVariantGroup,
        List<ExternalResponsibleEntity> externalResponsibleProducer,
        List<ExternalResponsibleEntity> externalResponsiblePerson,
        Optional<String> externalMetaProductId,
        Optional<ExternalProductSet> externalProductSets,
        List<ProductAttribute> attributes,
        List<List<ProductCategory>> categories,
        Optional<ProductSet> productSets,
        List<ProductAttachment> productAttachments,
        Optional<Translations> translations,
        DispatchTime dispatchTime,
        Optional<String> deliveryPriceList,
        Optional<BigDecimal> weight,
        Optional<Packaging> packaging,
        Optional<Integer> basketLimit,
        Optional<InvoiceType> invoiceType,
        Optional<TaxRate> taxRate,
        Optional<String> obligatoryIdentifier,
        Optional<String> voluntaryIdentifier,
        Optional<String> returnIdentifier,
        Optional<String> energyLabel,
        Optional<String> instructionWithSafetyInformation,
        Optional<String> informationCard,
        Optional<BigDecimal> producerId,
        List<BigDecimal> producerIds,
        Optional<BigDecimal> responsiblePersonId,
        List<BigDecimal> responsiblePersonIds,
        Optional<Integer> sourceFulfillmentProductId,
        Optional<Integer> automaticDiscountRuleId,
        boolean archived,
        Optional<OffsetDateTime> archivedAt,
        FrozenFields frozen,
        List<String> buyableProblems,
        OffsetDateTime created,
        Optional<OffsetDateTime> updated) {

    public Product {
        importantFeatures = List.copyOf(importantFeatures);
        images = List.copyOf(images);
        files = List.copyOf(files);
        externalReferences = List.copyOf(externalReferences);
        externalAttributes = List.copyOf(externalAttributes);
        externalCategories = List.copyOf(externalCategories);
        externalResponsibleProducer = List.copyOf(externalResponsibleProducer);
        externalResponsiblePerson = List.copyOf(externalResponsiblePerson);
        attributes = List.copyOf(attributes);
        categories = categories.stream().map(List::copyOf).toList();
        productAttachments = List.copyOf(productAttachments);
        producerIds = List.copyOf(producerIds);
        responsiblePersonIds = List.copyOf(responsiblePersonIds);
        buyableProblems = List.copyOf(buyableProblems);
    }

    /**
     * Whether the marketplace will currently sell this product — it is active, not archived, and the
     * marketplace reported no blocking problems.
     */
    public boolean isBuyable() {
        return status == ProductStatus.ACTIVE && !archived && buyableProblems.isEmpty();
    }
}
