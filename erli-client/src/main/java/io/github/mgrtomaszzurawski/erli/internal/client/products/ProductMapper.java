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

    static Product toDomain(ProductResponse raw) {
        Objects.requireNonNull(raw, "raw ProductResponse");
        return new Product(
                externalId(raw),
                require(raw.getMarketplaceId(), "marketplaceId").longValue(),
                require(raw.getName(), "name"),
                require(raw.getSlug(), "slug"),
                ProductEnums.toProductStatus(require(raw.getStatus(), "status").getValue()),
                require(raw.getStock(), "stock"),
                ProductValues.toMoney(require(raw.getPrice(), "price")),
                ProductValues.toOptionalMoney(raw.getMobilePrice()),
                ProductValues.toOptionalMoney(raw.getCataloguePrice()),
                ProductValues.mapOptional(raw.getReferencePriceType(),
                        value -> ProductEnums.toReferencePriceType(value.getValue())),
                ProductValues.mapOptional(raw.getDescription(), ProductMapper::toDescription),
                Optional.ofNullable(raw.getExternalDescription()),
                Optional.ofNullable(raw.getExternalDescriptionHash()),
                Optional.ofNullable(raw.getEan()),
                Optional.ofNullable(raw.getSku()),
                ProductValues.mapOptional(raw.getBaseMarket(),
                        value -> ProductEnums.toBaseMarket(value.getValue())),
                ProductValues.mapOptional(raw.getMarkets(),
                        value -> ProductEnums.toMarket(value.getValue())),
                ProductValues.orEmpty(raw.getImportantFeatures()),
                ProductValues.mapEach(raw.getImages(), ProductMapper::toImage),
                ProductValues.mapEach(raw.getFiles(), ProductMapper::toFile),
                ProductValues.mapEach(raw.getExternalReferences(), ProductMapper::toExternalReference),
                ProductValues.mapEach(raw.getExternalAttributes(), ProductMapper::toExternalAttribute),
                ProductValues.mapEach(raw.getExternalCategories(), ProductMapper::toExternalCategory),
                ProductValues.mapOptional(raw.getExternalVariantGroup(), ProductMapper::toVariantGroup),
                ProductValues.mapEach(raw.getExternalResponsibleProducer(), ProductMapper::toResponsibleProducer),
                ProductValues.mapEach(raw.getExternalResponsiblePerson(), ProductMapper::toResponsiblePerson),
                Optional.ofNullable(raw.getExternalMetaProductId()),
                ProductValues.mapOptional(raw.getExternalProductSets(), ProductMapper::toExternalProductSet),
                ProductValues.mapEach(raw.getAttributes(), ProductMapper::toAttribute),
                toCategoryPaths(raw),
                ProductValues.mapOptional(raw.getProductSets(), ProductMapper::toProductSet),
                ProductValues.mapEach(raw.getProductAttachments(), ProductMapper::toAttachment),
                ProductValues.mapOptional(raw.getTranslations(), ProductMapper::toTranslations),
                toDispatchTime(require(raw.getDispatchTime(), "dispatchTime")),
                Optional.ofNullable(raw.getDeliveryPriceList()),
                Optional.ofNullable(raw.getWeight()),
                ProductValues.mapOptional(raw.getPackaging(), ProductMapper::toPackaging),
                Optional.ofNullable(raw.getBasketLimit()),
                ProductValues.mapOptional(raw.getInvoiceType(),
                        value -> ProductEnums.toInvoiceType(value.getValue())),
                ProductValues.mapOptional(raw.getTaxRate(), value -> ProductEnums.toTaxRate(value.getValue())),
                Optional.ofNullable(raw.getObligatoryIdentifier()),
                Optional.ofNullable(raw.getVoluntaryIdentifier()),
                Optional.ofNullable(raw.getReturnIdentifier()),
                Optional.ofNullable(raw.getEnergyLabel()),
                Optional.ofNullable(raw.getInstructionWithSafetyInformation()),
                Optional.ofNullable(raw.getInformationCard()),
                Optional.ofNullable(raw.getProducerId()),
                ProductValues.orEmpty(raw.getProducerIds()),
                Optional.ofNullable(raw.getResponsiblePersonId()),
                ProductValues.orEmpty(raw.getResponsiblePersonIds()),
                Optional.ofNullable(raw.getSourceFulfillmentProductId()),
                Optional.ofNullable(raw.getAutomaticDiscountRuleId()),
                Boolean.TRUE.equals(raw.getArchived()),
                Optional.ofNullable(raw.getArchivedAt()),
                toFrozenFields(require(raw.getFrozen(), "frozen")),
                ProductValues.orEmpty(raw.getBuyableProblems()),
                require(raw.getCreated(), "created"),
                Optional.ofNullable(raw.getUpdated()));
    }

    private static ProductExternalId externalId(ProductResponse raw) {
        Object value = require(raw.getExternalId(), "externalId").getActualInstance();
        return ProductExternalId.of(String.valueOf(value));
    }

    private static ProductDescription toDescription(ProductCreateDescriptionAnyOf raw) {
        return new ProductDescription(ProductValues.mapEach(raw.getSections(), ProductMapper::toSection));
    }

    private static DescriptionSection toSection(ProductCreateDescriptionAnyOfSectionsInner raw) {
        return new DescriptionSection(ProductValues.mapEach(raw.getItems(), ProductMapper::toDescriptionItem));
    }

    private static DescriptionItem toDescriptionItem(ProductCreateDescriptionAnyOfSectionsInnerItemsInner raw) {
        return new DescriptionItem(
                ProductEnums.toDescriptionItemType(require(raw.getType(), "description item type").getValue()),
                Optional.ofNullable(raw.getContent()),
                Optional.ofNullable(raw.getUrl()));
    }

    private static ProductImage toImage(ProductResponseImagesInner raw) {
        return new ProductImage(
                require(raw.getUrl(), "image url"),
                Optional.ofNullable(raw.getIsVariantImage()),
                Optional.ofNullable(raw.getIsLifestyleImage()),
                Optional.ofNullable(raw.getIsFrozenImage()),
                Optional.ofNullable(raw.getOriginalExternalUrl()),
                ProductValues.mapOptional(raw.getAppliedTransformation(),
                        value -> ProductEnums.toImageTransformation(value.getValue())),
                Optional.ofNullable(raw.getInternalUrl()));
    }

    private static ProductFile toFile(ProductCreateFilesInner raw) {
        return new ProductFile(require(raw.getUrl(), "file url"));
    }

    private static ExternalReference toExternalReference(ProductCreateExternalReferencesInner raw) {
        Object instance = raw.getActualInstance();
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

    private static ExternalAttribute toExternalAttribute(ProductCreateExternalAttributesInner raw) {
        Object instance = raw.getActualInstance();
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

    private static ExternalCategory toExternalCategory(ProductCreateExternalCategoriesInner raw) {
        return new ExternalCategory(
                ProductValues.mapOptional(raw.getSource(), value -> ProductEnums.toExternalSource(value.getValue())),
                ProductValues.mapEach(raw.getBreadcrumb(), entry -> new DictionaryValue(
                        AttributeValueMapper.identifierText(entry.getId()),
                        Optional.ofNullable(entry.getName()))),
                Optional.ofNullable(raw.getIndex()));
    }

    private static ExternalVariantGroup toVariantGroup(ProductCreateExternalVariantGroup raw) {
        return new ExternalVariantGroup(
                Optional.ofNullable(raw.getId()),
                ProductValues.mapOptional(raw.getSource(),
                        value -> ProductEnums.toVariantGroupSource(value.getValue())),
                ProductValues.mapEach(raw.getAttributes(),
                        attribute -> String.valueOf(attribute.getActualInstance())));
    }

    private static ExternalResponsibleEntity toResponsibleProducer(
            ProductCreateExternalResponsibleProducerInner raw) {
        return new ExternalResponsibleEntity(
                Optional.ofNullable(raw.getExternalId()),
                ProductValues.mapOptional(raw.getSource(),
                        value -> ProductEnums.toResponsibleEntitySource(value.getValue())));
    }

    private static ExternalResponsibleEntity toResponsiblePerson(ProductCreateExternalResponsiblePersonInner raw) {
        return new ExternalResponsibleEntity(
                Optional.ofNullable(raw.getExternalId()),
                ProductValues.mapOptional(raw.getSource(),
                        value -> ProductEnums.toResponsibleEntitySource(value.getValue())));
    }

    private static ExternalProductSet toExternalProductSet(ProductCreateExternalProductSets raw) {
        return new ExternalProductSet(ProductValues.mapEach(raw.getItems(),
                item -> new ExternalProductSetItem(
                        Optional.ofNullable(item.getExternalMetaProductId()),
                        Optional.ofNullable(item.getQuantity()))));
    }

    private static ProductSet toProductSet(ProductCreateProductSets raw) {
        return new ProductSet(ProductValues.mapEach(raw.getItems(),
                item -> new ProductSetItem(
                        Optional.ofNullable(item.getMetaProductId()),
                        Optional.ofNullable(item.getQuantity()))));
    }

    private static ProductAttribute toAttribute(ProductResponseAttributesInner raw) {
        return new ProductAttribute(
                Optional.ofNullable(raw.getId()),
                Optional.ofNullable(raw.getName()),
                AttributeValueMapper.fromUntyped(raw.getValues()),
                ProductValues.orEmpty(raw.getValueIds()),
                Optional.ofNullable(raw.getUnit()));
    }

    private static List<List<ProductCategory>> toCategoryPaths(ProductResponse raw) {
        List<List<io.github.mgrtomaszzurawski.erli.rest.model.ProductResponseCategoriesInnerInner>> paths =
                raw.getCategories();
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

    private static ProductAttachment toAttachment(ProductCreateProductAttachmentsInner raw) {
        return new ProductAttachment(
                Optional.ofNullable(raw.getId()),
                ProductValues.mapOptional(raw.getKind(), value -> ProductEnums.toAttachmentKind(value.getValue())),
                Optional.ofNullable(raw.getUrl()),
                ProductValues.mapEach(raw.getMarkets(), ProductEnums::toMarket));
    }

    private static Translations toTranslations(ProductResponseTranslations raw) {
        return new Translations(
                ProductValues.mapOptional(raw.getPl(), ProductMapper::toTranslation),
                ProductValues.mapOptional(raw.getDe(), ProductMapper::toTranslation));
    }

    private static Translation toTranslation(ProductResponseTranslationsPl raw) {
        return new Translation(
                Optional.ofNullable(raw.getName()),
                Optional.ofNullable(raw.getDescriptionId()),
                ProductValues.mapEach(raw.getAttributes(), attribute -> new TranslatedAttribute(
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
    private static DispatchTime toDispatchTime(ProductCreateDispatchTime raw) {
        int period = ((Number) require(raw.getPeriod(), "dispatchTime period").getActualInstance()).intValue();
        DispatchTimeUnit unit = raw.getUnit() == null
                ? DispatchTime.DEFAULT_UNIT
                : ProductEnums.toDispatchTimeUnit(raw.getUnit().getValue());
        return new DispatchTime(unit, period);
    }

    private static Packaging toPackaging(ProductCreatePackaging raw) {
        return new Packaging(ProductValues.orEmpty(raw.getTags()), Optional.ofNullable(raw.getWeight()));
    }

    /**
     * The {@code frozen} object is a flag per freezable field; collect the flags that are set into the
     * domain's {@link FrozenFields} set.
     */
    private static FrozenFields toFrozenFields(ProductResponseFrozen raw) {
        Set<ProductField> frozen = EnumSet.noneOf(ProductField.class);
        addIfFrozen(frozen, ProductField.NAME, raw.getName());
        addIfFrozen(frozen, ProductField.DESCRIPTION, raw.getDescription());
        addIfFrozen(frozen, ProductField.EAN, raw.getEan());
        addIfFrozen(frozen, ProductField.SKU, raw.getSku());
        addIfFrozen(frozen, ProductField.EXTERNAL_ATTRIBUTES, raw.getExternalAttributes());
        addIfFrozen(frozen, ProductField.EXTERNAL_CATEGORIES, raw.getExternalCategories());
        addIfFrozen(frozen, ProductField.EXTERNAL_VARIANT_GROUP, raw.getExternalVariantGroup());
        addIfFrozen(frozen, ProductField.IMAGES, raw.getImages());
        addIfFrozen(frozen, ProductField.FILES, raw.getFiles());
        addIfFrozen(frozen, ProductField.PRICE, raw.getPrice());
        addIfFrozen(frozen, ProductField.MOBILE_PRICE, raw.getMobilePrice());
        addIfFrozen(frozen, ProductField.CATALOGUE_PRICE, raw.getCataloguePrice());
        addIfFrozen(frozen, ProductField.IMPORTANT_FEATURES, raw.getImportantFeatures());
        addIfFrozen(frozen, ProductField.STOCK, raw.getStock());
        addIfFrozen(frozen, ProductField.STATUS, raw.getStatus());
        addIfFrozen(frozen, ProductField.DISPATCH_TIME, raw.getDispatchTime());
        addIfFrozen(frozen, ProductField.INVOICE_TYPE, raw.getInvoiceType());
        addIfFrozen(frozen, ProductField.TAX_RATE, raw.getTaxRate());
        addIfFrozen(frozen, ProductField.OBLIGATORY_IDENTIFIER, raw.getObligatoryIdentifier());
        addIfFrozen(frozen, ProductField.VOLUNTARY_IDENTIFIER, raw.getVoluntaryIdentifier());
        addIfFrozen(frozen, ProductField.RETURN_IDENTIFIER, raw.getReturnIdentifier());
        addIfFrozen(frozen, ProductField.DELIVERY_PRICE_LIST, raw.getDeliveryPriceList());
        addIfFrozen(frozen, ProductField.WEIGHT, raw.getWeight());
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
