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
 * a caller can act on. Everything else degrades to an empty {@link Optional} or an empty list.
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
                ProductValues.mapOptional(rawProduct.getMarkets(),
                        value -> ProductEnums.toMarket(value.getValue())),
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

    private static ProductDescription toDescription(ProductCreateDescriptionAnyOf rawProduct) {
        return new ProductDescription(ProductValues.mapEach(rawProduct.getSections(), ProductMapper::toSection));
    }

    private static DescriptionSection toSection(ProductCreateDescriptionAnyOfSectionsInner rawProduct) {
        return new DescriptionSection(ProductValues.mapEach(rawProduct.getItems(), ProductMapper::toDescriptionItem));
    }

    private static DescriptionItem toDescriptionItem(ProductCreateDescriptionAnyOfSectionsInnerItemsInner rawProduct) {
        return new DescriptionItem(
                ProductEnums.toDescriptionItemType(require(rawProduct.getType(), "description item type").getValue()),
                Optional.ofNullable(rawProduct.getContent()),
                Optional.ofNullable(rawProduct.getUrl()));
    }

    private static ProductImage toImage(ProductResponseImagesInner rawProduct) {
        return new ProductImage(
                require(rawProduct.getUrl(), "image url"),
                Optional.ofNullable(rawProduct.getIsVariantImage()),
                Optional.ofNullable(rawProduct.getIsLifestyleImage()),
                Optional.ofNullable(rawProduct.getIsFrozenImage()),
                Optional.ofNullable(rawProduct.getOriginalExternalUrl()),
                ProductValues.mapOptional(rawProduct.getAppliedTransformation(),
                        value -> ProductEnums.toImageTransformation(value.getValue())),
                Optional.ofNullable(rawProduct.getInternalUrl()));
    }

    private static ProductFile toFile(ProductCreateFilesInner rawProduct) {
        return new ProductFile(require(rawProduct.getUrl(), "file url"));
    }

    private static ExternalReference toExternalReference(ProductCreateExternalReferencesInner rawProduct) {
        Object instance = rawProduct.getActualInstance();
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

    private static ExternalAttribute toExternalAttribute(ProductCreateExternalAttributesInner rawProduct) {
        Object instance = rawProduct.getActualInstance();
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

    private static ExternalCategory toExternalCategory(ProductCreateExternalCategoriesInner rawProduct) {
        return new ExternalCategory(
                ProductValues.mapOptional(rawProduct.getSource(), value -> ProductEnums.toExternalSource(value.getValue())),
                ProductValues.mapEach(rawProduct.getBreadcrumb(), entry -> new DictionaryValue(
                        AttributeValueMapper.identifierText(entry.getId()),
                        Optional.ofNullable(entry.getName()))),
                Optional.ofNullable(rawProduct.getIndex()));
    }

    private static ExternalVariantGroup toVariantGroup(ProductCreateExternalVariantGroup rawProduct) {
        return new ExternalVariantGroup(
                Optional.ofNullable(rawProduct.getId()),
                ProductValues.mapOptional(rawProduct.getSource(),
                        value -> ProductEnums.toVariantGroupSource(value.getValue())),
                ProductValues.mapEach(rawProduct.getAttributes(),
                        attribute -> String.valueOf(attribute.getActualInstance())));
    }

    private static ExternalResponsibleEntity toResponsibleProducer(
            ProductCreateExternalResponsibleProducerInner rawProduct) {
        return new ExternalResponsibleEntity(
                Optional.ofNullable(rawProduct.getExternalId()),
                ProductValues.mapOptional(rawProduct.getSource(),
                        value -> ProductEnums.toResponsibleEntitySource(value.getValue())));
    }

    private static ExternalResponsibleEntity toResponsiblePerson(ProductCreateExternalResponsiblePersonInner rawProduct) {
        return new ExternalResponsibleEntity(
                Optional.ofNullable(rawProduct.getExternalId()),
                ProductValues.mapOptional(rawProduct.getSource(),
                        value -> ProductEnums.toResponsibleEntitySource(value.getValue())));
    }

    private static ExternalProductSet toExternalProductSet(ProductCreateExternalProductSets rawProduct) {
        return new ExternalProductSet(ProductValues.mapEach(rawProduct.getItems(),
                item -> new ExternalProductSetItem(
                        Optional.ofNullable(item.getExternalMetaProductId()),
                        Optional.ofNullable(item.getQuantity()))));
    }

    private static ProductSet toProductSet(ProductCreateProductSets rawProduct) {
        return new ProductSet(ProductValues.mapEach(rawProduct.getItems(),
                item -> new ProductSetItem(
                        Optional.ofNullable(item.getMetaProductId()),
                        Optional.ofNullable(item.getQuantity()))));
    }

    private static ProductAttribute toAttribute(ProductResponseAttributesInner rawProduct) {
        return new ProductAttribute(
                Optional.ofNullable(rawProduct.getId()),
                Optional.ofNullable(rawProduct.getName()),
                AttributeValueMapper.fromUntyped(rawProduct.getValues()),
                ProductValues.orEmpty(rawProduct.getValueIds()),
                Optional.ofNullable(rawProduct.getUnit()));
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

    private static ProductAttachment toAttachment(ProductCreateProductAttachmentsInner rawProduct) {
        return new ProductAttachment(
                Optional.ofNullable(rawProduct.getId()),
                ProductValues.mapOptional(rawProduct.getKind(), value -> ProductEnums.toAttachmentKind(value.getValue())),
                Optional.ofNullable(rawProduct.getUrl()),
                ProductValues.mapEach(rawProduct.getMarkets(), ProductEnums::toMarket));
    }

    private static Translations toTranslations(ProductResponseTranslations rawProduct) {
        return new Translations(
                ProductValues.mapOptional(rawProduct.getPl(), ProductMapper::toTranslation),
                ProductValues.mapOptional(rawProduct.getDe(), ProductMapper::toTranslation));
    }

    private static Translation toTranslation(ProductResponseTranslationsPl rawProduct) {
        return new Translation(
                Optional.ofNullable(rawProduct.getName()),
                Optional.ofNullable(rawProduct.getDescriptionId()),
                ProductValues.mapEach(rawProduct.getAttributes(), attribute -> new TranslatedAttribute(
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
    private static DispatchTime toDispatchTime(ProductCreateDispatchTime rawProduct) {
        int period = ((Number) require(rawProduct.getPeriod(), "dispatchTime period").getActualInstance()).intValue();
        DispatchTimeUnit unit = rawProduct.getUnit() == null
                ? DispatchTime.DEFAULT_UNIT
                : ProductEnums.toDispatchTimeUnit(rawProduct.getUnit().getValue());
        return new DispatchTime(unit, period);
    }

    private static Packaging toPackaging(ProductCreatePackaging rawProduct) {
        return new Packaging(ProductValues.orEmpty(rawProduct.getTags()), Optional.ofNullable(rawProduct.getWeight()));
    }

    /**
     * The {@code frozen} object is a flag per freezable field; collect the flags that are set into the
     * domain's {@link FrozenFields} set.
     */
    private static FrozenFields toFrozenFields(ProductResponseFrozen rawProduct) {
        Set<ProductField> frozen = EnumSet.noneOf(ProductField.class);
        addIfFrozen(frozen, ProductField.NAME, rawProduct.getName());
        addIfFrozen(frozen, ProductField.DESCRIPTION, rawProduct.getDescription());
        addIfFrozen(frozen, ProductField.EAN, rawProduct.getEan());
        addIfFrozen(frozen, ProductField.SKU, rawProduct.getSku());
        addIfFrozen(frozen, ProductField.EXTERNAL_ATTRIBUTES, rawProduct.getExternalAttributes());
        addIfFrozen(frozen, ProductField.EXTERNAL_CATEGORIES, rawProduct.getExternalCategories());
        addIfFrozen(frozen, ProductField.EXTERNAL_VARIANT_GROUP, rawProduct.getExternalVariantGroup());
        addIfFrozen(frozen, ProductField.IMAGES, rawProduct.getImages());
        addIfFrozen(frozen, ProductField.FILES, rawProduct.getFiles());
        addIfFrozen(frozen, ProductField.PRICE, rawProduct.getPrice());
        addIfFrozen(frozen, ProductField.MOBILE_PRICE, rawProduct.getMobilePrice());
        addIfFrozen(frozen, ProductField.CATALOGUE_PRICE, rawProduct.getCataloguePrice());
        addIfFrozen(frozen, ProductField.IMPORTANT_FEATURES, rawProduct.getImportantFeatures());
        addIfFrozen(frozen, ProductField.STOCK, rawProduct.getStock());
        addIfFrozen(frozen, ProductField.STATUS, rawProduct.getStatus());
        addIfFrozen(frozen, ProductField.DISPATCH_TIME, rawProduct.getDispatchTime());
        addIfFrozen(frozen, ProductField.INVOICE_TYPE, rawProduct.getInvoiceType());
        addIfFrozen(frozen, ProductField.TAX_RATE, rawProduct.getTaxRate());
        addIfFrozen(frozen, ProductField.OBLIGATORY_IDENTIFIER, rawProduct.getObligatoryIdentifier());
        addIfFrozen(frozen, ProductField.VOLUNTARY_IDENTIFIER, rawProduct.getVoluntaryIdentifier());
        addIfFrozen(frozen, ProductField.RETURN_IDENTIFIER, rawProduct.getReturnIdentifier());
        addIfFrozen(frozen, ProductField.DELIVERY_PRICE_LIST, rawProduct.getDeliveryPriceList());
        addIfFrozen(frozen, ProductField.WEIGHT, rawProduct.getWeight());
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

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalStateException("ProductResponse is missing the required '" + field + "' field");
        }
        return value;
    }
}
