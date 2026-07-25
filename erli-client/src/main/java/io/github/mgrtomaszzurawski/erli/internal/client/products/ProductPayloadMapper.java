package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.domain.products.AttributeValues;
import io.github.mgrtomaszzurawski.erli.domain.products.DescriptionItem;
import io.github.mgrtomaszzurawski.erli.domain.products.DescriptionSection;
import io.github.mgrtomaszzurawski.erli.domain.products.DictionaryValue;
import io.github.mgrtomaszzurawski.erli.domain.products.DispatchTime;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalAttribute;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalCategory;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalProductSet;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalReference;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalResponsibleEntity;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalVariantGroup;
import io.github.mgrtomaszzurawski.erli.domain.products.FrozenFields;
import io.github.mgrtomaszzurawski.erli.domain.products.Packaging;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductAttachment;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDescription;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductFile;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductImage;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSet;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDescription;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDescriptionAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDescriptionAnyOfSectionsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDescriptionAnyOfSectionsInnerItemsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDispatchTime;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateDispatchTimePeriod;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf1Values;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf2;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf2ValuesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOf3;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalAttributesInnerAnyOfId;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalCategoriesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalProductSets;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalProductSetsItemsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalReferencesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalReferencesInnerAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalResponsiblePersonInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalResponsibleProducerInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalVariantGroup;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateExternalVariantGroupAttributesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateFilesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateImagesInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreatePackaging;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateProductAttachmentsInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateProductSets;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateProductSetsItemsInner;

import java.math.BigDecimal;
import java.util.List;

/**
 * Converts the domain's nested write types into their Layer-1 counterparts. Shared by the create and
 * update mappers, which differ in how they treat the top-level fields, not in how a description or an
 * attribute is shaped. Internal: never exported.
 */
final class ProductPayloadMapper {

    private ProductPayloadMapper() {
    }

    static ProductCreateDescription description(ProductDescription description) {
        ProductCreateDescriptionAnyOf structured = new ProductCreateDescriptionAnyOf();
        structured.setSections(description.sections().stream()
                .map(ProductPayloadMapper::section)
                .toList());
        return new ProductCreateDescription(structured);
    }

    private static ProductCreateDescriptionAnyOfSectionsInner section(DescriptionSection section) {
        ProductCreateDescriptionAnyOfSectionsInner raw = new ProductCreateDescriptionAnyOfSectionsInner();
        raw.setItems(section.items().stream().map(ProductPayloadMapper::descriptionItem).toList());
        return raw;
    }

    private static ProductCreateDescriptionAnyOfSectionsInnerItemsInner descriptionItem(DescriptionItem item) {
        ProductCreateDescriptionAnyOfSectionsInnerItemsInner raw =
                new ProductCreateDescriptionAnyOfSectionsInnerItemsInner();
        raw.setType(ProductCreateDescriptionAnyOfSectionsInnerItemsInner.TypeEnum
                .fromValue(ProductEnums.wireNameOf(item.type())));
        item.content().ifPresent(raw::setContent);
        item.url().ifPresent(raw::setUrl);
        return raw;
    }

    static List<ProductCreateImagesInner> images(List<ProductImage> images) {
        return images.stream().map(image -> {
            ProductCreateImagesInner raw = new ProductCreateImagesInner();
            raw.setUrl(image.url());
            image.isVariantImage().ifPresent(raw::setIsVariantImage);
            image.isLifestyleImage().ifPresent(raw::setIsLifestyleImage);
            return raw;
        }).toList();
    }

    static List<ProductCreateFilesInner> files(List<ProductFile> files) {
        return files.stream().map(file -> {
            ProductCreateFilesInner raw = new ProductCreateFilesInner();
            raw.setUrl(file.url());
            return raw;
        }).toList();
    }

    static ProductCreateDispatchTime dispatchTime(DispatchTime dispatchTime) {
        ProductCreateDispatchTime raw = new ProductCreateDispatchTime();
        raw.setUnit(ProductCreateDispatchTime.UnitEnum.fromValue(ProductEnums.wireNameOf(dispatchTime.unit())));
        raw.setPeriod(new ProductCreateDispatchTimePeriod(dispatchTime.period()));
        return raw;
    }

    static ProductCreatePackaging packaging(Packaging packaging) {
        ProductCreatePackaging raw = new ProductCreatePackaging();
        raw.setTags(packaging.tags());
        packaging.weight().ifPresent(raw::setWeight);
        return raw;
    }

    static List<ProductCreateExternalReferencesInner> externalReferences(List<ExternalReference> references) {
        return references.stream().map(reference -> {
            ProductCreateExternalReferencesInnerAnyOf raw = new ProductCreateExternalReferencesInnerAnyOf();
            reference.id().ifPresent(raw::setId);
            reference.url().ifPresent(raw::setUrl);
            reference.kind().ifPresent(kind -> raw.setKind(
                    ProductCreateExternalReferencesInnerAnyOf.KindEnum.fromValue(ProductEnums.wireNameOf(kind))));
            reference.source().ifPresent(source -> raw.setSource(
                    ProductCreateExternalReferencesInnerAnyOf.SourceEnum.fromValue(
                            ProductEnums.wireNameOf(source))));
            return new ProductCreateExternalReferencesInner(raw);
        }).toList();
    }

    /**
     * Attributes are an {@code anyOf} whose branch is chosen by the shape of the values, so the sealed
     * {@link AttributeValues} hierarchy selects the Layer-1 branch directly — the domain type carries the
     * information the wire format needs, with no guessing.
     */
    static List<ProductCreateExternalAttributesInner> externalAttributes(List<ExternalAttribute> attributes) {
        return attributes.stream().map(ProductPayloadMapper::externalAttribute).toList();
    }

    private static ProductCreateExternalAttributesInner externalAttribute(ExternalAttribute attribute) {
        // Pattern matching for switch is a Java 21 preview-turned-standard feature; the SDK's baseline is
        // Java 17, so this dispatches with instanceof patterns instead. AttributeValues is sealed, so the
        // final throw is unreachable unless a new variant is added without a branch here.
        AttributeValues values = attribute.values();
        if (values instanceof AttributeValues.NumericValues numeric) {
            ProductCreateExternalAttributesInnerAnyOf raw = new ProductCreateExternalAttributesInnerAnyOf();
            attribute.id().ifPresent(id -> raw.setId(identifier(id)));
            attribute.name().ifPresent(raw::setName);
            attribute.index().ifPresent(raw::setIndex);
            attribute.unit().ifPresent(raw::setUnit);
            attribute.source().ifPresent(source -> raw.setSource(
                    ProductCreateExternalAttributesInnerAnyOf.SourceEnum.fromValue(
                            ProductEnums.wireNameOf(source))));
            attribute.type().ifPresent(type -> raw.setType(
                    ProductCreateExternalAttributesInnerAnyOf.TypeEnum.fromValue(
                            ProductEnums.wireNameOf(type))));
            raw.setValues(numeric.numbers());
            return new ProductCreateExternalAttributesInner(raw);
        }
        if (values instanceof AttributeValues.RangeValues range) {
            ProductCreateExternalAttributesInnerAnyOf1 raw = new ProductCreateExternalAttributesInnerAnyOf1();
            attribute.id().ifPresent(id -> raw.setId(identifier(id)));
            attribute.name().ifPresent(raw::setName);
            attribute.index().ifPresent(raw::setIndex);
            attribute.unit().ifPresent(raw::setUnit);
            attribute.source().ifPresent(source -> raw.setSource(
                    ProductCreateExternalAttributesInnerAnyOf1.SourceEnum.fromValue(
                            ProductEnums.wireNameOf(source))));
            attribute.type().ifPresent(type -> raw.setType(
                    ProductCreateExternalAttributesInnerAnyOf1.TypeEnum.fromValue(
                            ProductEnums.wireNameOf(type))));
            ProductCreateExternalAttributesInnerAnyOf1Values bounds =
                    new ProductCreateExternalAttributesInnerAnyOf1Values();
            range.from().ifPresent(bounds::setFrom);
            range.to().ifPresent(bounds::setTo);
            raw.setValues(bounds);
            return new ProductCreateExternalAttributesInner(raw);
        }
        if (values instanceof AttributeValues.DictionaryValues dictionary) {
            ProductCreateExternalAttributesInnerAnyOf2 raw = new ProductCreateExternalAttributesInnerAnyOf2();
            attribute.id().ifPresent(id -> raw.setId(identifier(id)));
            attribute.name().ifPresent(raw::setName);
            attribute.index().ifPresent(raw::setIndex);
            attribute.source().ifPresent(source -> raw.setSource(
                    ProductCreateExternalAttributesInnerAnyOf2.SourceEnum.fromValue(
                            ProductEnums.wireNameOf(source))));
            attribute.type().ifPresent(type -> raw.setType(
                    ProductCreateExternalAttributesInnerAnyOf2.TypeEnum.fromValue(
                            ProductEnums.wireNameOf(type))));
            raw.setValues(dictionary.entries().stream()
                    .map(ProductPayloadMapper::dictionaryEntry)
                    .toList());
            return new ProductCreateExternalAttributesInner(raw);
        }
        if (values instanceof AttributeValues.TextValues text) {
            ProductCreateExternalAttributesInnerAnyOf3 raw = new ProductCreateExternalAttributesInnerAnyOf3();
            attribute.id().ifPresent(id -> raw.setId(identifier(id)));
            attribute.name().ifPresent(raw::setName);
            attribute.index().ifPresent(raw::setIndex);
            attribute.source().ifPresent(source -> raw.setSource(
                    ProductCreateExternalAttributesInnerAnyOf3.SourceEnum.fromValue(
                            ProductEnums.wireNameOf(source))));
            attribute.type().ifPresent(type -> raw.setType(
                    ProductCreateExternalAttributesInnerAnyOf3.TypeEnum.fromValue(
                            ProductEnums.wireNameOf(type))));
            raw.setValues(text.texts());
            return new ProductCreateExternalAttributesInner(raw);
        }
        throw new IllegalStateException("Unhandled AttributeValues variant: " + values.getClass());
    }

    private static ProductCreateExternalAttributesInnerAnyOf2ValuesInner dictionaryEntry(DictionaryValue entry) {
        ProductCreateExternalAttributesInnerAnyOf2ValuesInner raw =
                new ProductCreateExternalAttributesInnerAnyOf2ValuesInner();
        raw.setId(identifier(entry.id()));
        entry.name().ifPresent(raw::setName);
        return raw;
    }

    /**
     * A dictionary identifier is a string or a number on the wire. The domain keeps it as text, so a
     * value that is wholly numeric is sent as a number and everything else as a string — which is what
     * the marketplace echoes back for ids it owns.
     */
    private static ProductCreateExternalAttributesInnerAnyOfId identifier(String value) {
        try {
            return new ProductCreateExternalAttributesInnerAnyOfId(new BigDecimal(value));
        } catch (NumberFormatException notNumeric) {
            return new ProductCreateExternalAttributesInnerAnyOfId(value);
        }
    }

    static List<ProductCreateExternalCategoriesInner> externalCategories(List<ExternalCategory> categories) {
        return categories.stream().map(category -> {
            ProductCreateExternalCategoriesInner raw = new ProductCreateExternalCategoriesInner();
            category.index().ifPresent(raw::setIndex);
            category.source().ifPresent(source -> raw.setSource(
                    ProductCreateExternalCategoriesInner.SourceEnum.fromValue(ProductEnums.wireNameOf(source))));
            raw.setBreadcrumb(category.breadcrumb().stream()
                    .map(ProductPayloadMapper::dictionaryEntry)
                    .toList());
            return raw;
        }).toList();
    }

    static ProductCreateExternalVariantGroup externalVariantGroup(ExternalVariantGroup group) {
        ProductCreateExternalVariantGroup raw = new ProductCreateExternalVariantGroup();
        group.id().ifPresent(raw::setId);
        group.source().ifPresent(source -> raw.setSource(
                ProductCreateExternalVariantGroup.SourceEnum.fromValue(ProductEnums.wireNameOf(source))));
        raw.setAttributes(group.attributes().stream()
                .map(ProductCreateExternalVariantGroupAttributesInner::new)
                .toList());
        return raw;
    }

    static List<ProductCreateExternalResponsibleProducerInner> responsibleProducers(
            List<ExternalResponsibleEntity> entities) {
        return entities.stream().map(entity -> {
            ProductCreateExternalResponsibleProducerInner raw =
                    new ProductCreateExternalResponsibleProducerInner();
            entity.externalId().ifPresent(raw::setExternalId);
            entity.source().ifPresent(source -> raw.setSource(
                    ProductCreateExternalResponsibleProducerInner.SourceEnum.fromValue(
                            ProductEnums.wireNameOf(source))));
            return raw;
        }).toList();
    }

    static List<ProductCreateExternalResponsiblePersonInner> responsiblePersons(
            List<ExternalResponsibleEntity> entities) {
        return entities.stream().map(entity -> {
            ProductCreateExternalResponsiblePersonInner raw = new ProductCreateExternalResponsiblePersonInner();
            entity.externalId().ifPresent(raw::setExternalId);
            entity.source().ifPresent(source -> raw.setSource(
                    ProductCreateExternalResponsiblePersonInner.SourceEnum.fromValue(
                            ProductEnums.wireNameOf(source))));
            return raw;
        }).toList();
    }

    static ProductCreateExternalProductSets externalProductSets(ExternalProductSet set) {
        ProductCreateExternalProductSets raw = new ProductCreateExternalProductSets();
        raw.setItems(set.items().stream().map(item -> {
            ProductCreateExternalProductSetsItemsInner rawItem = new ProductCreateExternalProductSetsItemsInner();
            item.externalMetaProductId().ifPresent(rawItem::setExternalMetaProductId);
            item.quantity().ifPresent(rawItem::setQuantity);
            return rawItem;
        }).toList());
        return raw;
    }

    static ProductCreateProductSets productSets(ProductSet set) {
        ProductCreateProductSets raw = new ProductCreateProductSets();
        raw.setItems(set.items().stream().map(item -> {
            ProductCreateProductSetsItemsInner rawItem = new ProductCreateProductSetsItemsInner();
            item.metaProductId().ifPresent(rawItem::setMetaProductId);
            item.quantity().ifPresent(rawItem::setQuantity);
            return rawItem;
        }).toList());
        return raw;
    }

    static List<ProductCreateProductAttachmentsInner> productAttachments(List<ProductAttachment> attachments) {
        return attachments.stream().map(attachment -> {
            ProductCreateProductAttachmentsInner raw = new ProductCreateProductAttachmentsInner();
            attachment.id().ifPresent(raw::setId);
            attachment.url().ifPresent(raw::setUrl);
            attachment.kind().ifPresent(kind -> raw.setKind(
                    ProductCreateProductAttachmentsInner.KindEnum.fromValue(ProductEnums.wireNameOf(kind))));
            raw.setMarkets(attachment.markets().stream().map(ProductEnums::wireNameOf).toList());
            return raw;
        }).toList();
    }

    /** Whether a field is pinned, for the boolean-per-field {@code frozen} object. */
    static Boolean frozenFlag(FrozenFields frozen, ProductField field) {
        return frozen.isFrozen(field) ? Boolean.TRUE : null;
    }
}
