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
import io.github.mgrtomaszzurawski.erli.domain.products.Market;
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
        ProductCreateDescriptionAnyOfSectionsInner rawPayload = new ProductCreateDescriptionAnyOfSectionsInner();
        rawPayload.setItems(section.items().stream().map(ProductPayloadMapper::descriptionItem).toList());
        return rawPayload;
    }

    private static ProductCreateDescriptionAnyOfSectionsInnerItemsInner descriptionItem(DescriptionItem item) {
        ProductCreateDescriptionAnyOfSectionsInnerItemsInner rawPayload =
                new ProductCreateDescriptionAnyOfSectionsInnerItemsInner();
        rawPayload.setType(ProductCreateDescriptionAnyOfSectionsInnerItemsInner.TypeEnum
                .fromValue(item.type().wireName()));
        item.content().ifPresent(rawPayload::setContent);
        item.url().ifPresent(rawPayload::setUrl);
        return rawPayload;
    }

    static List<ProductCreateImagesInner> images(List<ProductImage> images) {
        return images.stream().map(image -> {
            ProductCreateImagesInner rawPayload = new ProductCreateImagesInner();
            rawPayload.setUrl(image.url());
            image.isVariantImage().ifPresent(rawPayload::setIsVariantImage);
            image.isLifestyleImage().ifPresent(rawPayload::setIsLifestyleImage);
            return rawPayload;
        }).toList();
    }

    static List<ProductCreateFilesInner> files(List<ProductFile> files) {
        return files.stream().map(file -> {
            ProductCreateFilesInner rawPayload = new ProductCreateFilesInner();
            rawPayload.setUrl(file.url());
            return rawPayload;
        }).toList();
    }

    static ProductCreateDispatchTime dispatchTime(DispatchTime dispatchTime) {
        ProductCreateDispatchTime rawPayload = new ProductCreateDispatchTime();
        rawPayload.setUnit(ProductCreateDispatchTime.UnitEnum.fromValue(dispatchTime.unit().wireName()));
        rawPayload.setPeriod(new ProductCreateDispatchTimePeriod(dispatchTime.period()));
        return rawPayload;
    }

    static ProductCreatePackaging packaging(Packaging packaging) {
        ProductCreatePackaging rawPayload = new ProductCreatePackaging();
        rawPayload.setTags(packaging.tags());
        packaging.weight().ifPresent(rawPayload::setWeight);
        return rawPayload;
    }

    static List<ProductCreateExternalReferencesInner> externalReferences(List<ExternalReference> references) {
        return references.stream().map(reference -> {
            ProductCreateExternalReferencesInnerAnyOf rawPayload = new ProductCreateExternalReferencesInnerAnyOf();
            reference.id().ifPresent(rawPayload::setId);
            reference.url().ifPresent(rawPayload::setUrl);
            reference.kind().ifPresent(kind -> rawPayload.setKind(
                    ProductCreateExternalReferencesInnerAnyOf.KindEnum.fromValue(kind.wireName())));
            reference.source().ifPresent(source -> rawPayload.setSource(
                    ProductCreateExternalReferencesInnerAnyOf.SourceEnum.fromValue(
                            source.wireName())));
            return new ProductCreateExternalReferencesInner(rawPayload);
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
            ProductCreateExternalAttributesInnerAnyOf rawPayload = new ProductCreateExternalAttributesInnerAnyOf();
            attribute.id().ifPresent(id -> rawPayload.setId(identifier(id)));
            attribute.name().ifPresent(rawPayload::setName);
            attribute.index().ifPresent(rawPayload::setIndex);
            attribute.unit().ifPresent(rawPayload::setUnit);
            attribute.source().ifPresent(source -> rawPayload.setSource(
                    ProductCreateExternalAttributesInnerAnyOf.SourceEnum.fromValue(
                            source.wireName())));
            attribute.type().ifPresent(type -> rawPayload.setType(
                    ProductCreateExternalAttributesInnerAnyOf.TypeEnum.fromValue(
                            type.wireName())));
            rawPayload.setValues(numeric.numbers());
            return new ProductCreateExternalAttributesInner(rawPayload);
        }
        if (values instanceof AttributeValues.RangeValues range) {
            ProductCreateExternalAttributesInnerAnyOf1 rawPayload = new ProductCreateExternalAttributesInnerAnyOf1();
            attribute.id().ifPresent(id -> rawPayload.setId(identifier(id)));
            attribute.name().ifPresent(rawPayload::setName);
            attribute.index().ifPresent(rawPayload::setIndex);
            attribute.unit().ifPresent(rawPayload::setUnit);
            attribute.source().ifPresent(source -> rawPayload.setSource(
                    ProductCreateExternalAttributesInnerAnyOf1.SourceEnum.fromValue(
                            source.wireName())));
            attribute.type().ifPresent(type -> rawPayload.setType(
                    ProductCreateExternalAttributesInnerAnyOf1.TypeEnum.fromValue(
                            type.wireName())));
            ProductCreateExternalAttributesInnerAnyOf1Values bounds =
                    new ProductCreateExternalAttributesInnerAnyOf1Values();
            range.from().ifPresent(bounds::setFrom);
            range.to().ifPresent(bounds::setTo);
            rawPayload.setValues(bounds);
            return new ProductCreateExternalAttributesInner(rawPayload);
        }
        if (values instanceof AttributeValues.DictionaryValues dictionary) {
            ProductCreateExternalAttributesInnerAnyOf2 rawPayload = new ProductCreateExternalAttributesInnerAnyOf2();
            attribute.id().ifPresent(id -> rawPayload.setId(identifier(id)));
            attribute.name().ifPresent(rawPayload::setName);
            attribute.index().ifPresent(rawPayload::setIndex);
            attribute.source().ifPresent(source -> rawPayload.setSource(
                    ProductCreateExternalAttributesInnerAnyOf2.SourceEnum.fromValue(
                            source.wireName())));
            attribute.type().ifPresent(type -> rawPayload.setType(
                    ProductCreateExternalAttributesInnerAnyOf2.TypeEnum.fromValue(
                            type.wireName())));
            rawPayload.setValues(dictionary.entries().stream()
                    .map(ProductPayloadMapper::dictionaryEntry)
                    .toList());
            return new ProductCreateExternalAttributesInner(rawPayload);
        }
        if (values instanceof AttributeValues.TextValues text) {
            ProductCreateExternalAttributesInnerAnyOf3 rawPayload = new ProductCreateExternalAttributesInnerAnyOf3();
            attribute.id().ifPresent(id -> rawPayload.setId(identifier(id)));
            attribute.name().ifPresent(rawPayload::setName);
            attribute.index().ifPresent(rawPayload::setIndex);
            attribute.source().ifPresent(source -> rawPayload.setSource(
                    ProductCreateExternalAttributesInnerAnyOf3.SourceEnum.fromValue(
                            source.wireName())));
            attribute.type().ifPresent(type -> rawPayload.setType(
                    ProductCreateExternalAttributesInnerAnyOf3.TypeEnum.fromValue(
                            type.wireName())));
            rawPayload.setValues(text.texts());
            return new ProductCreateExternalAttributesInner(rawPayload);
        }
        throw new IllegalStateException("Unhandled AttributeValues variant: " + values.getClass());
    }

    private static ProductCreateExternalAttributesInnerAnyOf2ValuesInner dictionaryEntry(DictionaryValue entry) {
        ProductCreateExternalAttributesInnerAnyOf2ValuesInner rawPayload =
                new ProductCreateExternalAttributesInnerAnyOf2ValuesInner();
        rawPayload.setId(identifier(entry.id()));
        entry.name().ifPresent(rawPayload::setName);
        return rawPayload;
    }

    /**
     * A dictionary identifier is a string or a number on the wire. The domain keeps it as text, so a
     * value that is wholly numeric is sent as a number and everything else as a string — which is what
     * the marketplace echoes back for ids it owns.
     */
    private static ProductCreateExternalAttributesInnerAnyOfId identifier(String value) {
        if (value == null) {
            // The read side leaves a dictionary id absent when the marketplace omitted it, so a
            // read-modify-write round trip legitimately arrives here with null. Sending no id is the
            // faithful echo of that; constructing a BigDecimal from null would throw NullPointerException,
            // which the catch below does not cover.
            return null;
        }
        try {
            return new ProductCreateExternalAttributesInnerAnyOfId(new BigDecimal(value));
        } catch (NumberFormatException notNumeric) {
            return new ProductCreateExternalAttributesInnerAnyOfId(value);
        }
    }

    static List<ProductCreateExternalCategoriesInner> externalCategories(List<ExternalCategory> categories) {
        return categories.stream().map(category -> {
            ProductCreateExternalCategoriesInner rawPayload = new ProductCreateExternalCategoriesInner();
            category.index().ifPresent(rawPayload::setIndex);
            category.source().ifPresent(source -> rawPayload.setSource(
                    ProductCreateExternalCategoriesInner.SourceEnum.fromValue(source.wireName())));
            rawPayload.setBreadcrumb(category.breadcrumb().stream()
                    .map(ProductPayloadMapper::dictionaryEntry)
                    .toList());
            return rawPayload;
        }).toList();
    }

    static ProductCreateExternalVariantGroup externalVariantGroup(ExternalVariantGroup group) {
        ProductCreateExternalVariantGroup rawPayload = new ProductCreateExternalVariantGroup();
        group.id().ifPresent(rawPayload::setId);
        group.source().ifPresent(source -> rawPayload.setSource(
                ProductCreateExternalVariantGroup.SourceEnum.fromValue(source.wireName())));
        rawPayload.setAttributes(group.attributes().stream()
                .map(ProductCreateExternalVariantGroupAttributesInner::new)
                .toList());
        return rawPayload;
    }

    static List<ProductCreateExternalResponsibleProducerInner> responsibleProducers(
            List<ExternalResponsibleEntity> entities) {
        return entities.stream().map(entity -> {
            ProductCreateExternalResponsibleProducerInner rawPayload =
                    new ProductCreateExternalResponsibleProducerInner();
            entity.externalId().ifPresent(rawPayload::setExternalId);
            entity.source().ifPresent(source -> rawPayload.setSource(
                    ProductCreateExternalResponsibleProducerInner.SourceEnum.fromValue(
                            source.wireName())));
            return rawPayload;
        }).toList();
    }

    static List<ProductCreateExternalResponsiblePersonInner> responsiblePersons(
            List<ExternalResponsibleEntity> entities) {
        return entities.stream().map(entity -> {
            ProductCreateExternalResponsiblePersonInner rawPayload = new ProductCreateExternalResponsiblePersonInner();
            entity.externalId().ifPresent(rawPayload::setExternalId);
            entity.source().ifPresent(source -> rawPayload.setSource(
                    ProductCreateExternalResponsiblePersonInner.SourceEnum.fromValue(
                            source.wireName())));
            return rawPayload;
        }).toList();
    }

    static ProductCreateExternalProductSets externalProductSets(ExternalProductSet set) {
        ProductCreateExternalProductSets rawPayload = new ProductCreateExternalProductSets();
        rawPayload.setItems(set.items().stream().map(item -> {
            ProductCreateExternalProductSetsItemsInner rawItem = new ProductCreateExternalProductSetsItemsInner();
            item.externalMetaProductId().ifPresent(rawItem::setExternalMetaProductId);
            item.quantity().ifPresent(rawItem::setQuantity);
            return rawItem;
        }).toList());
        return rawPayload;
    }

    static ProductCreateProductSets productSets(ProductSet set) {
        ProductCreateProductSets rawPayload = new ProductCreateProductSets();
        rawPayload.setItems(set.items().stream().map(item -> {
            ProductCreateProductSetsItemsInner rawItem = new ProductCreateProductSetsItemsInner();
            item.metaProductId().ifPresent(rawItem::setMetaProductId);
            item.quantity().ifPresent(rawItem::setQuantity);
            return rawItem;
        }).toList());
        return rawPayload;
    }

    static List<ProductCreateProductAttachmentsInner> productAttachments(List<ProductAttachment> attachments) {
        return attachments.stream().map(attachment -> {
            ProductCreateProductAttachmentsInner rawPayload = new ProductCreateProductAttachmentsInner();
            attachment.id().ifPresent(rawPayload::setId);
            attachment.url().ifPresent(rawPayload::setUrl);
            attachment.kind().ifPresent(kind -> rawPayload.setKind(
                    ProductCreateProductAttachmentsInner.KindEnum.fromValue(kind.wireName())));
            rawPayload.setMarkets(attachment.markets().stream().map(Market::wireName).toList());
            return rawPayload;
        }).toList();
    }

    /** Whether a field is pinned, for the boolean-per-field {@code frozen} object. */
    static Boolean frozenFlag(FrozenFields frozen, ProductField field) {
        return frozen.isFrozen(field) ? Boolean.TRUE : null;
    }
}
