package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.domain.products.AttributeValues;
import io.github.mgrtomaszzurawski.erli.domain.products.DescriptionItem;
import io.github.mgrtomaszzurawski.erli.domain.products.DescriptionSection;
import io.github.mgrtomaszzurawski.erli.domain.products.DictionaryValue;
import io.github.mgrtomaszzurawski.erli.domain.products.DispatchTime;
import io.github.mgrtomaszzurawski.erli.domain.products.DispatchTimeUnit;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalAttribute;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalCategory;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalProductSet;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalProductSetItem;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalReference;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalResponsibleEntity;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalVariantGroup;
import io.github.mgrtomaszzurawski.erli.domain.products.FrozenFields;
import io.github.mgrtomaszzurawski.erli.domain.products.Market;
import io.github.mgrtomaszzurawski.erli.domain.products.Packaging;
import io.github.mgrtomaszzurawski.erli.domain.products.Product;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAttachment;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAttribute;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductCategory;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDescription;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFile;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductImage;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSet;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSetItem;
import io.github.mgrtomaszzurawski.erli.domain.products.TranslatedAttribute;
import io.github.mgrtomaszzurawski.erli.domain.products.Translation;
import io.github.mgrtomaszzurawski.erli.domain.products.Translations;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDescriptionAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDescriptionAnyOfSectionsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDescriptionAnyOfSectionsInnerItemsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDispatchTime;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf2;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf3;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalCategoriesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalProductSets;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalReferencesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalReferencesInnerAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalResponsiblePersonInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalResponsibleProducerInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalVariantGroup;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateFilesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreatePackaging;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateProductAttachmentsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateProductSets;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponseAttributesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponseFrozen;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponseImagesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponseTranslations;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponseTranslationsPl;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Maps the generated Layer-1 {@link ProductResponse} to the public {@link Product} domain record.
 *
 * <p>Fields the spec marks required are demanded here: a missing one throws rather than yielding a
 * {@code Product} with a silent {@code null}, because a product without a price or an id is not something
 * a caller can act on. Everything else degrades to an empty {@link Optional} or an empty list. For an
 * enum-backed field "missing" also covers "sent, but unrecognised" — see {@link #require}.
 *
 * <p>Kept in an internal package so no {@code *Raw} type ever appears in an exported signature.
 * Internal: never exported.
 */
final class ProductMapper {

    private ProductMapper() {
    }

    static Product toDomain(ProductResponse rawProduct) {
        Objects.requireNonNull(rawProduct, "ProductResponse");
        return new Product(
                externalId(rawProduct),
                require(rawProduct.getMarketplaceId(), "marketplaceId").longValue(),
                require(rawProduct.getName(), "name"),
                require(rawProduct.getSlug(), "slug"),
                ProductEnums.toProductStatus(require(rawProduct.getStatus(), "status").getValue()),
                require(rawProduct.getStock(), "stock"),
                ProductValues.toMoney(require(rawProduct.getPrice(), "price")),
                ProductValues.toOptionalMoney(rawProduct.getMobilePrice()),
                ProductValues.toOptionalMoney(rawProduct.getCataloguePrice()),
                ProductValues.mapOptional(rawProduct.getReferencePriceType(),
                        value -> ProductEnums.toReferencePriceType(value.getValue())),
                ProductValues.mapOptional(rawProduct.getDescription(), ProductMapper::toDescription),
                Optional.ofNullable(rawProduct.getExternalDescription()),
                Optional.ofNullable(rawProduct.getExternalDescriptionHash()),
                Optional.ofNullable(rawProduct.getEan()),
                Optional.ofNullable(rawProduct.getSku()),
                ProductValues.mapOptional(rawProduct.getBaseMarket(),
                        value -> ProductEnums.toBaseMarket(value.getValue())),
                // Scalar here, despite the plural name: Layer 1 types it as an enum, so an unknown
                // value has already become absent (CORE-12) and the lookup always resolves.
                ProductValues.mapOptional(rawProduct.getMarkets(),
                        value -> ProductEnums.toMarketOrNull(value.getValue())),
                ProductValues.orEmpty(rawProduct.getImportantFeatures()),
                ProductValues.mapEach(rawProduct.getImages(), ProductMapper::toImage),
                ProductValues.mapEach(rawProduct.getFiles(), ProductMapper::toFile),
                ProductValues.mapEach(rawProduct.getExternalReferences(), ProductMapper::toExternalReference),
                ProductValues.mapEach(rawProduct.getExternalAttributes(), ProductMapper::toExternalAttribute),
                ProductValues.mapEach(rawProduct.getExternalCategories(), ProductMapper::toExternalCategory),
                ProductValues.mapOptional(rawProduct.getExternalVariantGroup(), ProductMapper::toVariantGroup),
                ProductValues.mapEach(rawProduct.getExternalResponsibleProducer(), ProductMapper::toResponsibleProducer),
                ProductValues.mapEach(rawProduct.getExternalResponsiblePerson(), ProductMapper::toResponsiblePerson),
                Optional.ofNullable(rawProduct.getExternalMetaProductId()),
                ProductValues.mapOptional(rawProduct.getExternalProductSets(), ProductMapper::toExternalProductSet),
                ProductValues.mapEach(rawProduct.getAttributes(), ProductMapper::toAttribute),
                toCategoryPaths(rawProduct),
                ProductValues.mapOptional(rawProduct.getProductSets(), ProductMapper::toProductSet),
                ProductValues.mapEach(rawProduct.getProductAttachments(), ProductMapper::toAttachment),
                ProductValues.mapOptional(rawProduct.getTranslations(), ProductMapper::toTranslations),
                toDispatchTime(require(rawProduct.getDispatchTime(), "dispatchTime")),
                Optional.ofNullable(rawProduct.getDeliveryPriceList()),
                Optional.ofNullable(rawProduct.getWeight()),
                ProductValues.mapOptional(rawProduct.getPackaging(), ProductMapper::toPackaging),
                Optional.ofNullable(rawProduct.getBasketLimit()),
                ProductValues.mapOptional(rawProduct.getInvoiceType(),
                        value -> ProductEnums.toInvoiceType(value.getValue())),
                ProductValues.mapOptional(rawProduct.getTaxRate(), value -> ProductEnums.toTaxRate(value.getValue())),
                Optional.ofNullable(rawProduct.getObligatoryIdentifier()),
                Optional.ofNullable(rawProduct.getVoluntaryIdentifier()),
                Optional.ofNullable(rawProduct.getReturnIdentifier()),
                Optional.ofNullable(rawProduct.getEnergyLabel()),
                Optional.ofNullable(rawProduct.getInstructionWithSafetyInformation()),
                Optional.ofNullable(rawProduct.getInformationCard()),
                Optional.ofNullable(rawProduct.getProducerId()),
                ProductValues.orEmpty(rawProduct.getProducerIds()),
                Optional.ofNullable(rawProduct.getResponsiblePersonId()),
                ProductValues.orEmpty(rawProduct.getResponsiblePersonIds()),
                Optional.ofNullable(rawProduct.getSourceFulfillmentProductId()),
                Optional.ofNullable(rawProduct.getAutomaticDiscountRuleId()),
                Boolean.TRUE.equals(rawProduct.getArchived()),
                Optional.ofNullable(rawProduct.getArchivedAt()),
                toFrozenFields(require(rawProduct.getFrozen(), "frozen")),
                ProductValues.orEmpty(rawProduct.getBuyableProblems()),
                require(rawProduct.getCreated(), "created"),
                Optional.ofNullable(rawProduct.getUpdated()));
    }

    private static ProductExternalId externalId(ProductResponse rawProduct) {
        Object value = require(rawProduct.getExternalId(), "externalId").getActualInstance();
        return ProductExternalId.of(String.valueOf(value));
    }

    private static ProductDescription toDescription(ProductCreateDescriptionAnyOf rawDescription) {
        return new ProductDescription(ProductValues.mapEach(rawDescription.getSections(), ProductMapper::toSection));
    }

    private static DescriptionSection toSection(ProductCreateDescriptionAnyOfSectionsInner rawSection) {
        return new DescriptionSection(ProductValues.mapEach(rawSection.getItems(), ProductMapper::toDescriptionItem));
    }

    private static DescriptionItem toDescriptionItem(ProductCreateDescriptionAnyOfSectionsInnerItemsInner rawItem) {
        return new DescriptionItem(
                ProductEnums.toDescriptionItemType(require(rawItem.getType(), "description item type").getValue()),
                Optional.ofNullable(rawItem.getContent()),
                Optional.ofNullable(rawItem.getUrl()));
    }

    private static ProductImage toImage(ProductResponseImagesInner rawImage) {
        return new ProductImage(
                require(rawImage.getUrl(), "image url"),
                Optional.ofNullable(rawImage.getIsVariantImage()),
                Optional.ofNullable(rawImage.getIsLifestyleImage()),
                Optional.ofNullable(rawImage.getIsFrozenImage()),
                Optional.ofNullable(rawImage.getOriginalExternalUrl()),
                ProductValues.mapOptional(rawImage.getAppliedTransformation(),
                        value -> ProductEnums.toImageTransformation(value.getValue())),
                Optional.ofNullable(rawImage.getInternalUrl()));
    }

    private static ProductFile toFile(ProductCreateFilesInner rawFile) {
        return new ProductFile(require(rawFile.getUrl(), "file url"));
    }

    private static ExternalReference toExternalReference(ProductCreateExternalReferencesInner rawReference) {
        Object instance = rawReference.getActualInstance();
        if (instance instanceof ProductCreateExternalReferencesInnerAnyOf reference) {
            return new ExternalReference(
                    Optional.ofNullable(reference.getId()),
                    ProductValues.mapOptional(reference.getKind(),
                            value -> ProductEnums.toExternalReferenceKind(value.getValue())),
                    Optional.ofNullable(reference.getUrl()),
                    ProductValues.mapOptional(reference.getSource(),
                            value -> ProductEnums.toExternalReferenceSource(value.getValue())));
        }
        // The spec's other branch is a bare URL string.
        return new ExternalReference(Optional.empty(), Optional.empty(),
                Optional.ofNullable(instance).map(String::valueOf), Optional.empty());
    }

    private static ExternalAttribute toExternalAttribute(ProductCreateExternalAttributesInner rawAttribute) {
        Object instance = rawAttribute.getActualInstance();
        if (instance instanceof ProductCreateExternalAttributesInnerAnyOf numeric) {
            return externalAttribute(
                    AttributeValueMapper.identifierText(numeric.getId()), numeric.getName(),
                    valueOrNull(numeric.getSource()), valueOrNull(numeric.getType()), numeric.getIndex(),
                    AttributeValueMapper.fromNumbers(numeric.getValues()), numeric.getUnit());
        }
        if (instance instanceof ProductCreateExternalAttributesInnerAnyOf1 range) {
            return externalAttribute(
                    AttributeValueMapper.identifierText(range.getId()), range.getName(),
                    valueOrNull(range.getSource()), valueOrNull(range.getType()), range.getIndex(),
                    AttributeValueMapper.fromRange(range.getValues()), range.getUnit());
        }
        if (instance instanceof ProductCreateExternalAttributesInnerAnyOf2 dictionary) {
            return externalAttribute(
                    AttributeValueMapper.identifierText(dictionary.getId()), dictionary.getName(),
                    valueOrNull(dictionary.getSource()), valueOrNull(dictionary.getType()),
                    dictionary.getIndex(), AttributeValueMapper.fromDictionary(dictionary.getValues()), null);
        }
        if (instance instanceof ProductCreateExternalAttributesInnerAnyOf3 text) {
            return externalAttribute(
                    AttributeValueMapper.identifierText(text.getId()), text.getName(),
                    valueOrNull(text.getSource()), valueOrNull(text.getType()), text.getIndex(),
                    AttributeValueMapper.fromTexts(text.getValues()), null);
        }
        throw new IllegalStateException(
                "Unrecognised externalAttributes variant: " + (instance == null ? "null" : instance.getClass()));
    }

    private static ExternalAttribute externalAttribute(String id, String name, String source, String type,
            Integer index, AttributeValues values, String unit) {
        return new ExternalAttribute(
                Optional.ofNullable(id),
                Optional.ofNullable(name),
                Optional.ofNullable(source).map(ProductEnums::toExternalSource),
                Optional.ofNullable(type).map(ProductEnums::toExternalAttributeType),
                Optional.ofNullable(index),
                values,
                Optional.ofNullable(unit));
    }

    private static ExternalCategory toExternalCategory(ProductCreateExternalCategoriesInner rawCategory) {
        return new ExternalCategory(
                ProductValues.mapOptional(rawCategory.getSource(), value -> ProductEnums.toExternalSource(value.getValue())),
                ProductValues.mapEach(rawCategory.getBreadcrumb(), entry -> new DictionaryValue(
                        AttributeValueMapper.identifierText(entry.getId()),
                        Optional.ofNullable(entry.getName()))),
                Optional.ofNullable(rawCategory.getIndex()));
    }

    private static ExternalVariantGroup toVariantGroup(ProductCreateExternalVariantGroup rawGroup) {
        return new ExternalVariantGroup(
                Optional.ofNullable(rawGroup.getId()),
                ProductValues.mapOptional(rawGroup.getSource(),
                        value -> ProductEnums.toVariantGroupSource(value.getValue())),
                ProductValues.mapEach(rawGroup.getAttributes(),
                        attribute -> String.valueOf(attribute.getActualInstance())));
    }

    private static ExternalResponsibleEntity toResponsibleProducer(
            ProductCreateExternalResponsibleProducerInner rawProducer) {
        return new ExternalResponsibleEntity(
                Optional.ofNullable(rawProducer.getExternalId()),
                ProductValues.mapOptional(rawProducer.getSource(),
                        value -> ProductEnums.toResponsibleEntitySource(value.getValue())));
    }

    private static ExternalResponsibleEntity toResponsiblePerson(ProductCreateExternalResponsiblePersonInner rawPerson) {
        return new ExternalResponsibleEntity(
                Optional.ofNullable(rawPerson.getExternalId()),
                ProductValues.mapOptional(rawPerson.getSource(),
                        value -> ProductEnums.toResponsibleEntitySource(value.getValue())));
    }

    private static ExternalProductSet toExternalProductSet(ProductCreateExternalProductSets rawSet) {
        return new ExternalProductSet(ProductValues.mapEach(rawSet.getItems(),
                item -> new ExternalProductSetItem(
                        Optional.ofNullable(item.getExternalMetaProductId()),
                        Optional.ofNullable(item.getQuantity()))));
    }

    private static ProductSet toProductSet(ProductCreateProductSets rawSet) {
        return new ProductSet(ProductValues.mapEach(rawSet.getItems(),
                item -> new ProductSetItem(
                        Optional.ofNullable(item.getMetaProductId()),
                        Optional.ofNullable(item.getQuantity()))));
    }

    private static ProductAttribute toAttribute(ProductResponseAttributesInner rawAttribute) {
        return new ProductAttribute(
                Optional.ofNullable(rawAttribute.getId()),
                Optional.ofNullable(rawAttribute.getName()),
                AttributeValueMapper.fromUntyped(rawAttribute.getValues()),
                ProductValues.orEmpty(rawAttribute.getValueIds()),
                Optional.ofNullable(rawAttribute.getUnit()));
    }

    private static List<List<ProductCategory>> toCategoryPaths(ProductResponse rawProduct) {
        List<List<io.github.mgrtomaszzurawski.erli.rest.model.ProductResponseCategoriesInnerInner>> paths =
                rawProduct.getCategories();
        if (paths == null) {
            return List.of();
        }
        List<List<ProductCategory>> mapped = new ArrayList<>(paths.size());
        for (var path : paths) {
            mapped.add(ProductValues.mapEach(path, node -> new ProductCategory(
                    Optional.ofNullable(node.getId()),
                    Optional.ofNullable(node.getName()))));
        }
        return mapped;
    }

    private static ProductAttachment toAttachment(ProductCreateProductAttachmentsInner rawAttachment) {
        List<Market> knownMarkets = new ArrayList<>();
        List<String> unrecognisedMarkets = new ArrayList<>();
        splitMarkets(rawAttachment.getMarkets(), knownMarkets, unrecognisedMarkets);
        return new ProductAttachment(
                Optional.ofNullable(rawAttachment.getId()),
                ProductValues.mapOptional(rawAttachment.getKind(), value -> ProductEnums.toAttachmentKind(value.getValue())),
                Optional.ofNullable(rawAttachment.getUrl()),
                knownMarkets,
                unrecognisedMarkets);
    }

    /**
     * Split an attachment's market scope into the markets this SDK version can name and the raw wire
     * values it cannot, in one pass.
     *
     * <p>Keeping the unnameable ones rather than dropping them is what lets a caller read a product,
     * change something else and write it back without quietly narrowing the scope. Null entries are
     * skipped: the spec types these items as non-nullable strings, but a lookup on a null key would
     * throw, and one malformed entry must not fail the whole product read.
     */
    private static void splitMarkets(List<String> wireValues, List<Market> known,
            List<String> unrecognised) {
        for (String wireValue : ProductValues.orEmpty(wireValues)) {
            if (wireValue == null) {
                continue;
            }
            Market market = ProductEnums.toMarketOrNull(wireValue);
            if (market == null) {
                unrecognised.add(wireValue);
            } else {
                known.add(market);
            }
        }
    }

    private static Translations toTranslations(ProductResponseTranslations rawTranslations) {
        return new Translations(
                ProductValues.mapOptional(rawTranslations.getPl(), ProductMapper::toTranslation),
                ProductValues.mapOptional(rawTranslations.getDe(), ProductMapper::toTranslation));
    }

    private static Translation toTranslation(ProductResponseTranslationsPl rawTranslation) {
        return new Translation(
                Optional.ofNullable(rawTranslation.getName()),
                Optional.ofNullable(rawTranslation.getDescriptionId()),
                ProductValues.mapEach(rawTranslation.getAttributes(), attribute -> new TranslatedAttribute(
                        Optional.ofNullable(attribute.getKey()),
                        Optional.ofNullable(attribute.getName()),
                        AttributeValueMapper.fromUntyped(attribute.getValues()),
                        Optional.ofNullable(attribute.getUnit()))));
    }

    /**
     * {@code period} is the required half of {@code dispatchTime}; {@code unit} is optional and defaults
     * to working days, so a missing unit is passed through as {@code null} for
     * {@link DispatchTime} to resolve rather than being rejected here.
     */
    private static DispatchTime toDispatchTime(ProductCreateDispatchTime rawDispatchTime) {
        int period = ((Number) require(rawDispatchTime.getPeriod(), "dispatchTime period").getActualInstance()).intValue();
        DispatchTimeUnit unit = rawDispatchTime.getUnit() == null
                ? DispatchTime.DEFAULT_UNIT
                : ProductEnums.toDispatchTimeUnit(rawDispatchTime.getUnit().getValue());
        return new DispatchTime(unit, period);
    }

    private static Packaging toPackaging(ProductCreatePackaging rawPackaging) {
        return new Packaging(ProductValues.orEmpty(rawPackaging.getTags()), Optional.ofNullable(rawPackaging.getWeight()));
    }

    /**
     * The {@code frozen} object is a flag per freezable field; collect the flags that are set into the
     * domain's {@link FrozenFields} set.
     */
    private static FrozenFields toFrozenFields(ProductResponseFrozen rawFrozen) {
        Set<ProductField> frozen = EnumSet.noneOf(ProductField.class);
        addIfFrozen(frozen, ProductField.NAME, rawFrozen.getName());
        addIfFrozen(frozen, ProductField.DESCRIPTION, rawFrozen.getDescription());
        addIfFrozen(frozen, ProductField.EAN, rawFrozen.getEan());
        addIfFrozen(frozen, ProductField.SKU, rawFrozen.getSku());
        addIfFrozen(frozen, ProductField.EXTERNAL_ATTRIBUTES, rawFrozen.getExternalAttributes());
        addIfFrozen(frozen, ProductField.EXTERNAL_CATEGORIES, rawFrozen.getExternalCategories());
        addIfFrozen(frozen, ProductField.EXTERNAL_VARIANT_GROUP, rawFrozen.getExternalVariantGroup());
        addIfFrozen(frozen, ProductField.IMAGES, rawFrozen.getImages());
        addIfFrozen(frozen, ProductField.FILES, rawFrozen.getFiles());
        addIfFrozen(frozen, ProductField.PRICE, rawFrozen.getPrice());
        addIfFrozen(frozen, ProductField.MOBILE_PRICE, rawFrozen.getMobilePrice());
        addIfFrozen(frozen, ProductField.CATALOGUE_PRICE, rawFrozen.getCataloguePrice());
        addIfFrozen(frozen, ProductField.IMPORTANT_FEATURES, rawFrozen.getImportantFeatures());
        addIfFrozen(frozen, ProductField.STOCK, rawFrozen.getStock());
        addIfFrozen(frozen, ProductField.STATUS, rawFrozen.getStatus());
        addIfFrozen(frozen, ProductField.DISPATCH_TIME, rawFrozen.getDispatchTime());
        addIfFrozen(frozen, ProductField.INVOICE_TYPE, rawFrozen.getInvoiceType());
        addIfFrozen(frozen, ProductField.TAX_RATE, rawFrozen.getTaxRate());
        addIfFrozen(frozen, ProductField.OBLIGATORY_IDENTIFIER, rawFrozen.getObligatoryIdentifier());
        addIfFrozen(frozen, ProductField.VOLUNTARY_IDENTIFIER, rawFrozen.getVoluntaryIdentifier());
        addIfFrozen(frozen, ProductField.RETURN_IDENTIFIER, rawFrozen.getReturnIdentifier());
        addIfFrozen(frozen, ProductField.DELIVERY_PRICE_LIST, rawFrozen.getDeliveryPriceList());
        addIfFrozen(frozen, ProductField.WEIGHT, rawFrozen.getWeight());
        return FrozenFields.of(frozen);
    }

    private static void addIfFrozen(Set<ProductField> frozen, ProductField field, Boolean flag) {
        if (Boolean.TRUE.equals(flag)) {
            frozen.add(field);
        }
    }

    /** The wire value of a Layer-1 enum, or {@code null} when absent. */
    private static String valueOrNull(Object rawEnum) {
        if (rawEnum == null) {
            return null;
        }
        return String.valueOf(rawEnum);
    }

    /**
     * Demand a field the spec marks required.
     *
     * <p>The message names both causes on purpose. Since CORE-12 the codec decodes an unrecognised enum
     * value to {@code null}, so for an enum-backed field this fires when the value was absent
     * <em>or</em> when the marketplace sent one this SDK version does not know — and the two are
     * indistinguishable by the time the payload reaches here. Staying loud is the right answer for a
     * required field either way: a product whose status or dispatch time cannot be read is not
     * something a caller can act on.
     */
    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalStateException("ProductResponse is missing the required '" + field
                    + "' field, or carries a value this SDK version does not recognise");
        }
        return value;
    }
}
