package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.domain.products.ProductContent;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDraft;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductPatch;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreate;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductUpdate;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductUpdateFrozen;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductsBatchUpdatePatchRequestInner;

/**
 * Builds the {@code ProductCreate} and {@code ProductUpdate} request payloads from the domain's write
 * types.
 *
 * <p>The update path is where the care is. Layer 1 models a nullable property as {@code JsonNullable},
 * which has three states, and the SDK maps its three intentions straight onto them: a field never
 * touched stays {@code undefined} and is omitted; a field given a value is set; a field named in
 * {@link ProductPatch#cleared()} is set to {@code null}, which serializes as an explicit null and tells
 * the marketplace to remove it. Erli treats an explicit null as "clear", so conflating it with "absent"
 * would let a partial update erase data it was never asked to touch.
 *
 * <p>Internal: never exported.
 */
final class ProductRequestMapper {

    private ProductRequestMapper() {
    }

    /** The create payload for {@code POST /products/{externalId}}. */
    static ProductCreate toCreate(ProductDraft draft) {
        ProductCreate raw = new ProductCreate();
        ProductContent content = draft.content();
        content.name().ifPresent(raw::setName);
        content.description().ifPresent(value -> raw.setDescription(ProductPayloadMapper.description(value)));
        content.ean().ifPresent(raw::setEan);
        content.sku().ifPresent(raw::setSku);
        content.baseMarket().ifPresent(value -> raw.setBaseMarket(ProductCreate.BaseMarketEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.externalReferences().ifPresent(value -> raw.setExternalReferences(ProductPayloadMapper.externalReferences(value)));
        content.sourceFulfillmentProductId().ifPresent(raw::setSourceFulfillmentProductId);
        content.importantFeatures().ifPresent(raw::setImportantFeatures);
        content.externalAttributes().ifPresent(value -> raw.setExternalAttributes(ProductPayloadMapper.externalAttributes(value)));
        content.externalCategories().ifPresent(value -> raw.setExternalCategories(ProductPayloadMapper.externalCategories(value)));
        content.externalVariantGroup().ifPresent(value -> raw.setExternalVariantGroup(ProductPayloadMapper.externalVariantGroup(value)));
        content.externalResponsibleProducer().ifPresent(value -> raw.setExternalResponsibleProducer(ProductPayloadMapper.responsibleProducers(value)));
        content.externalResponsiblePerson().ifPresent(value -> raw.setExternalResponsiblePerson(ProductPayloadMapper.responsiblePersons(value)));
        content.images().ifPresent(value -> raw.setImages(ProductPayloadMapper.images(value)));
        content.files().ifPresent(value -> raw.setFiles(ProductPayloadMapper.files(value)));
        content.price().ifPresent(value -> raw.setPrice(ProductValues.toGrosze(value)));
        content.mobilePrice().ifPresent(value -> raw.setMobilePrice(ProductValues.toGrosze(value)));
        content.cataloguePrice().ifPresent(value -> raw.setCataloguePrice(ProductValues.toGrosze(value)));
        content.referencePriceType().ifPresent(value -> raw.setReferencePriceType(ProductCreate.ReferencePriceTypeEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.stock().ifPresent(raw::setStock);
        content.status().ifPresent(value -> raw.setStatus(ProductCreate.StatusEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.archived().ifPresent(raw::setArchived);
        content.dispatchTime().ifPresent(value -> raw.setDispatchTime(ProductPayloadMapper.dispatchTime(value)));
        content.deliveryPriceList().ifPresent(raw::setDeliveryPriceList);
        content.weight().ifPresent(raw::setWeight);
        content.obligatoryIdentifier().ifPresent(raw::setObligatoryIdentifier);
        content.voluntaryIdentifier().ifPresent(raw::setVoluntaryIdentifier);
        content.returnIdentifier().ifPresent(raw::setReturnIdentifier);
        content.invoiceType().ifPresent(value -> raw.setInvoiceType(ProductCreate.InvoiceTypeEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.taxRate().ifPresent(value -> raw.setTaxRate(ProductCreate.TaxRateEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.basketLimit().ifPresent(raw::setBasketLimit);
        content.energyLabel().ifPresent(raw::setEnergyLabel);
        content.instructionWithSafetyInformation().ifPresent(raw::setInstructionWithSafetyInformation);
        content.informationCard().ifPresent(raw::setInformationCard);
        content.producerId().ifPresent(raw::setProducerId);
        content.responsiblePersonId().ifPresent(raw::setResponsiblePersonId);
        content.externalMetaProductId().ifPresent(raw::setExternalMetaProductId);
        content.externalProductSets().ifPresent(value -> raw.setExternalProductSets(ProductPayloadMapper.externalProductSets(value)));
        content.productSets().ifPresent(value -> raw.setProductSets(ProductPayloadMapper.productSets(value)));
        content.productAttachments().ifPresent(value -> raw.setProductAttachments(ProductPayloadMapper.productAttachments(value)));
        content.automaticDiscountRuleId().ifPresent(raw::setAutomaticDiscountRuleId);
        content.packaging().ifPresent(value -> raw.setPackaging(ProductPayloadMapper.packaging(value)));
        content.frozen().ifPresent(frozen -> raw.setFrozen(createFrozen(frozen)));
        dropUnsetCollections(content, value -> raw.setImages(null), value -> raw.setExternalReferences(null));
        return raw;
    }

    /** The update payload for {@code PATCH /products/{externalId}}. */
    static ProductUpdate toUpdate(ProductPatch patch) {
        ProductUpdate raw = new ProductUpdate();
        ProductContent content = patch.content();
        content.name().ifPresent(raw::setName);
        content.description().ifPresent(value -> raw.setDescription(ProductPayloadMapper.description(value)));
        content.ean().ifPresent(raw::setEan);
        content.sku().ifPresent(raw::setSku);
        content.baseMarket().ifPresent(value -> raw.setBaseMarket(ProductUpdate.BaseMarketEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.externalReferences().ifPresent(value -> raw.setExternalReferences(ProductPayloadMapper.externalReferences(value)));
        content.sourceFulfillmentProductId().ifPresent(raw::setSourceFulfillmentProductId);
        content.importantFeatures().ifPresent(raw::setImportantFeatures);
        content.externalAttributes().ifPresent(value -> raw.setExternalAttributes(ProductPayloadMapper.externalAttributes(value)));
        content.externalCategories().ifPresent(value -> raw.setExternalCategories(ProductPayloadMapper.externalCategories(value)));
        content.externalVariantGroup().ifPresent(value -> raw.setExternalVariantGroup(ProductPayloadMapper.externalVariantGroup(value)));
        content.externalResponsibleProducer().ifPresent(value -> raw.setExternalResponsibleProducer(ProductPayloadMapper.responsibleProducers(value)));
        content.externalResponsiblePerson().ifPresent(value -> raw.setExternalResponsiblePerson(ProductPayloadMapper.responsiblePersons(value)));
        content.images().ifPresent(value -> raw.setImages(ProductPayloadMapper.images(value)));
        content.files().ifPresent(value -> raw.setFiles(ProductPayloadMapper.files(value)));
        content.price().ifPresent(value -> raw.setPrice(ProductValues.toGrosze(value)));
        content.mobilePrice().ifPresent(value -> raw.setMobilePrice(ProductValues.toGrosze(value)));
        content.cataloguePrice().ifPresent(value -> raw.setCataloguePrice(ProductValues.toGrosze(value)));
        content.referencePriceType().ifPresent(value -> raw.setReferencePriceType(ProductUpdate.ReferencePriceTypeEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.stock().ifPresent(raw::setStock);
        content.status().ifPresent(value -> raw.setStatus(ProductUpdate.StatusEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.archived().ifPresent(raw::setArchived);
        content.dispatchTime().ifPresent(value -> raw.setDispatchTime(ProductPayloadMapper.dispatchTime(value)));
        content.deliveryPriceList().ifPresent(raw::setDeliveryPriceList);
        content.weight().ifPresent(raw::setWeight);
        content.obligatoryIdentifier().ifPresent(raw::setObligatoryIdentifier);
        content.voluntaryIdentifier().ifPresent(raw::setVoluntaryIdentifier);
        content.returnIdentifier().ifPresent(raw::setReturnIdentifier);
        content.invoiceType().ifPresent(value -> raw.setInvoiceType(ProductUpdate.InvoiceTypeEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.taxRate().ifPresent(value -> raw.setTaxRate(ProductUpdate.TaxRateEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.basketLimit().ifPresent(raw::setBasketLimit);
        content.energyLabel().ifPresent(raw::setEnergyLabel);
        content.instructionWithSafetyInformation().ifPresent(raw::setInstructionWithSafetyInformation);
        content.informationCard().ifPresent(raw::setInformationCard);
        content.producerId().ifPresent(raw::setProducerId);
        content.responsiblePersonId().ifPresent(raw::setResponsiblePersonId);
        content.externalMetaProductId().ifPresent(raw::setExternalMetaProductId);
        content.externalProductSets().ifPresent(value -> raw.setExternalProductSets(ProductPayloadMapper.externalProductSets(value)));
        content.productSets().ifPresent(value -> raw.setProductSets(ProductPayloadMapper.productSets(value)));
        content.productAttachments().ifPresent(value -> raw.setProductAttachments(ProductPayloadMapper.productAttachments(value)));
        content.automaticDiscountRuleId().ifPresent(raw::setAutomaticDiscountRuleId);
        content.packaging().ifPresent(value -> raw.setPackaging(ProductPayloadMapper.packaging(value)));
        content.frozen().ifPresent(frozen -> raw.setFrozen(updateFrozen(frozen)));
        patch.newExternalId().ifPresent(value -> raw.setNewExternalId(value.value()));
        if (patch.overrideFrozen()) {
            raw.setOverrideFrozen(Boolean.TRUE);
        }
        dropUnsetCollections(content, value -> raw.setImages(null), value -> raw.setExternalReferences(null));
        for (ProductField field : patch.cleared()) {
            clear(raw, field);
        }
        return raw;
    }

    /**
     * One entry of {@code PATCH /products/batch-update}: the same update payload plus the id it applies
     * to. The generated batch item is a flattened copy of {@code ProductUpdate} rather than a wrapper, so
     * the update is built once and copied across field by field.
     */
    static ProductsBatchUpdatePatchRequestInner toBatchEntry(
            io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId externalId, ProductPatch patch) {
        ProductsBatchUpdatePatchRequestInner raw = new ProductsBatchUpdatePatchRequestInner();
        ProductContent content = patch.content();
        content.name().ifPresent(raw::setName);
        content.description().ifPresent(value -> raw.setDescription(ProductPayloadMapper.description(value)));
        content.ean().ifPresent(raw::setEan);
        content.sku().ifPresent(raw::setSku);
        content.baseMarket().ifPresent(value -> raw.setBaseMarket(ProductsBatchUpdatePatchRequestInner.BaseMarketEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.externalReferences().ifPresent(value -> raw.setExternalReferences(ProductPayloadMapper.externalReferences(value)));
        content.sourceFulfillmentProductId().ifPresent(raw::setSourceFulfillmentProductId);
        content.importantFeatures().ifPresent(raw::setImportantFeatures);
        content.externalAttributes().ifPresent(value -> raw.setExternalAttributes(ProductPayloadMapper.externalAttributes(value)));
        content.externalCategories().ifPresent(value -> raw.setExternalCategories(ProductPayloadMapper.externalCategories(value)));
        content.externalVariantGroup().ifPresent(value -> raw.setExternalVariantGroup(ProductPayloadMapper.externalVariantGroup(value)));
        content.externalResponsibleProducer().ifPresent(value -> raw.setExternalResponsibleProducer(ProductPayloadMapper.responsibleProducers(value)));
        content.externalResponsiblePerson().ifPresent(value -> raw.setExternalResponsiblePerson(ProductPayloadMapper.responsiblePersons(value)));
        content.images().ifPresent(value -> raw.setImages(ProductPayloadMapper.images(value)));
        content.files().ifPresent(value -> raw.setFiles(ProductPayloadMapper.files(value)));
        content.price().ifPresent(value -> raw.setPrice(ProductValues.toGrosze(value)));
        content.mobilePrice().ifPresent(value -> raw.setMobilePrice(ProductValues.toGrosze(value)));
        content.cataloguePrice().ifPresent(value -> raw.setCataloguePrice(ProductValues.toGrosze(value)));
        content.referencePriceType().ifPresent(value -> raw.setReferencePriceType(ProductsBatchUpdatePatchRequestInner.ReferencePriceTypeEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.stock().ifPresent(raw::setStock);
        content.status().ifPresent(value -> raw.setStatus(ProductsBatchUpdatePatchRequestInner.StatusEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.archived().ifPresent(raw::setArchived);
        content.dispatchTime().ifPresent(value -> raw.setDispatchTime(ProductPayloadMapper.dispatchTime(value)));
        content.deliveryPriceList().ifPresent(raw::setDeliveryPriceList);
        content.weight().ifPresent(raw::setWeight);
        content.obligatoryIdentifier().ifPresent(raw::setObligatoryIdentifier);
        content.voluntaryIdentifier().ifPresent(raw::setVoluntaryIdentifier);
        content.returnIdentifier().ifPresent(raw::setReturnIdentifier);
        content.invoiceType().ifPresent(value -> raw.setInvoiceType(ProductsBatchUpdatePatchRequestInner.InvoiceTypeEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.taxRate().ifPresent(value -> raw.setTaxRate(ProductsBatchUpdatePatchRequestInner.TaxRateEnum.fromValue(ProductEnums.wireNameOf(value))));
        content.basketLimit().ifPresent(raw::setBasketLimit);
        content.energyLabel().ifPresent(raw::setEnergyLabel);
        content.instructionWithSafetyInformation().ifPresent(raw::setInstructionWithSafetyInformation);
        content.informationCard().ifPresent(raw::setInformationCard);
        content.producerId().ifPresent(raw::setProducerId);
        content.responsiblePersonId().ifPresent(raw::setResponsiblePersonId);
        content.externalMetaProductId().ifPresent(raw::setExternalMetaProductId);
        content.externalProductSets().ifPresent(value -> raw.setExternalProductSets(ProductPayloadMapper.externalProductSets(value)));
        content.productSets().ifPresent(value -> raw.setProductSets(ProductPayloadMapper.productSets(value)));
        content.productAttachments().ifPresent(value -> raw.setProductAttachments(ProductPayloadMapper.productAttachments(value)));
        content.automaticDiscountRuleId().ifPresent(raw::setAutomaticDiscountRuleId);
        content.packaging().ifPresent(value -> raw.setPackaging(ProductPayloadMapper.packaging(value)));
        content.frozen().ifPresent(frozen -> raw.setFrozen(updateFrozen(frozen)));
        patch.newExternalId().ifPresent(value -> raw.setNewExternalId(value.value()));
        if (patch.overrideFrozen()) {
            raw.setOverrideFrozen(Boolean.TRUE);
        }
        dropUnsetCollections(content, value -> raw.setImages(null), value -> raw.setExternalReferences(null));
        for (ProductField field : patch.cleared()) {
            clearBatchEntry(raw, field);
        }
        raw.setExternalId(externalId.value());
        return raw;
    }

    /**
     * Send an explicit null for a cleared field. The switch covers exactly
     * {@link ProductPatch#clearableFields()}; anything else was already rejected when the patch was
     * built, so reaching the default here means those two lists have drifted apart.
     */
    private static void clear(ProductUpdate raw, ProductField field) {
        switch (field) {
            case DESCRIPTION -> raw.setDescription(null);
            case EAN -> raw.setEan(null);
            case SKU -> raw.setSku(null);
            case BASE_MARKET -> raw.setBaseMarket(null);
            case SOURCE_FULFILLMENT_PRODUCT_ID -> raw.setSourceFulfillmentProductId(null);
            case IMPORTANT_FEATURES -> raw.setImportantFeatures(null);
            case EXTERNAL_ATTRIBUTES -> raw.setExternalAttributes(null);
            case EXTERNAL_CATEGORIES -> raw.setExternalCategories(null);
            case EXTERNAL_VARIANT_GROUP -> raw.setExternalVariantGroup(null);
            case EXTERNAL_RESPONSIBLE_PRODUCER -> raw.setExternalResponsibleProducer(null);
            case EXTERNAL_RESPONSIBLE_PERSON -> raw.setExternalResponsiblePerson(null);
            case FILES -> raw.setFiles(null);
            case MOBILE_PRICE -> raw.setMobilePrice(null);
            case CATALOGUE_PRICE -> raw.setCataloguePrice(null);
            case REFERENCE_PRICE_TYPE -> raw.setReferencePriceType(null);
            case STATUS -> raw.setStatus(null);
            case DELIVERY_PRICE_LIST -> raw.setDeliveryPriceList(null);
            case WEIGHT -> raw.setWeight(null);
            case OBLIGATORY_IDENTIFIER -> raw.setObligatoryIdentifier(null);
            case VOLUNTARY_IDENTIFIER -> raw.setVoluntaryIdentifier(null);
            case RETURN_IDENTIFIER -> raw.setReturnIdentifier(null);
            case INVOICE_TYPE -> raw.setInvoiceType(null);
            case TAX_RATE -> raw.setTaxRate(null);
            case BASKET_LIMIT -> raw.setBasketLimit(null);
            case ENERGY_LABEL -> raw.setEnergyLabel(null);
            case INSTRUCTION_WITH_SAFETY_INFORMATION -> raw.setInstructionWithSafetyInformation(null);
            case INFORMATION_CARD -> raw.setInformationCard(null);
            case EXTERNAL_META_PRODUCT_ID -> raw.setExternalMetaProductId(null);
            case EXTERNAL_PRODUCT_SETS -> raw.setExternalProductSets(null);
            case PRODUCT_SETS -> raw.setProductSets(null);
            case PRODUCT_ATTACHMENTS -> raw.setProductAttachments(null);
            case AUTOMATIC_DISCOUNT_RULE_ID -> raw.setAutomaticDiscountRuleId(null);
            default -> throw new IllegalStateException(
                    "Field '" + field + "' is not clearable but reached the update mapper");
        }
    }

    private static void clearBatchEntry(ProductsBatchUpdatePatchRequestInner raw, ProductField field) {
        switch (field) {
            case DESCRIPTION -> raw.setDescription(null);
            case EAN -> raw.setEan(null);
            case SKU -> raw.setSku(null);
            case BASE_MARKET -> raw.setBaseMarket(null);
            case SOURCE_FULFILLMENT_PRODUCT_ID -> raw.setSourceFulfillmentProductId(null);
            case IMPORTANT_FEATURES -> raw.setImportantFeatures(null);
            case EXTERNAL_ATTRIBUTES -> raw.setExternalAttributes(null);
            case EXTERNAL_CATEGORIES -> raw.setExternalCategories(null);
            case EXTERNAL_VARIANT_GROUP -> raw.setExternalVariantGroup(null);
            case EXTERNAL_RESPONSIBLE_PRODUCER -> raw.setExternalResponsibleProducer(null);
            case EXTERNAL_RESPONSIBLE_PERSON -> raw.setExternalResponsiblePerson(null);
            case FILES -> raw.setFiles(null);
            case MOBILE_PRICE -> raw.setMobilePrice(null);
            case CATALOGUE_PRICE -> raw.setCataloguePrice(null);
            case REFERENCE_PRICE_TYPE -> raw.setReferencePriceType(null);
            case STATUS -> raw.setStatus(null);
            case DELIVERY_PRICE_LIST -> raw.setDeliveryPriceList(null);
            case WEIGHT -> raw.setWeight(null);
            case OBLIGATORY_IDENTIFIER -> raw.setObligatoryIdentifier(null);
            case VOLUNTARY_IDENTIFIER -> raw.setVoluntaryIdentifier(null);
            case RETURN_IDENTIFIER -> raw.setReturnIdentifier(null);
            case INVOICE_TYPE -> raw.setInvoiceType(null);
            case TAX_RATE -> raw.setTaxRate(null);
            case BASKET_LIMIT -> raw.setBasketLimit(null);
            case ENERGY_LABEL -> raw.setEnergyLabel(null);
            case INSTRUCTION_WITH_SAFETY_INFORMATION -> raw.setInstructionWithSafetyInformation(null);
            case INFORMATION_CARD -> raw.setInformationCard(null);
            case EXTERNAL_META_PRODUCT_ID -> raw.setExternalMetaProductId(null);
            case EXTERNAL_PRODUCT_SETS -> raw.setExternalProductSets(null);
            case PRODUCT_SETS -> raw.setProductSets(null);
            case PRODUCT_ATTACHMENTS -> raw.setProductAttachments(null);
            case AUTOMATIC_DISCOUNT_RULE_ID -> raw.setAutomaticDiscountRuleId(null);
            default -> throw new IllegalStateException(
                    "Field '" + field + "' is not clearable but reached the batch mapper");
        }
    }

    private static io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateFrozen createFrozen(
            io.github.mgrtomaszzurawski.erli.domain.products.FrozenFields frozen) {
        var raw = new io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateFrozen();
        raw.setName(ProductPayloadMapper.frozenFlag(frozen, ProductField.NAME));
        raw.setDescription(ProductPayloadMapper.frozenFlag(frozen, ProductField.DESCRIPTION));
        raw.setEan(ProductPayloadMapper.frozenFlag(frozen, ProductField.EAN));
        raw.setSku(ProductPayloadMapper.frozenFlag(frozen, ProductField.SKU));
        raw.setExternalAttributes(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_ATTRIBUTES));
        raw.setExternalCategories(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_CATEGORIES));
        raw.setExternalVariantGroup(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_VARIANT_GROUP));
        raw.setImages(ProductPayloadMapper.frozenFlag(frozen, ProductField.IMAGES));
        raw.setFiles(ProductPayloadMapper.frozenFlag(frozen, ProductField.FILES));
        raw.setPrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.PRICE));
        raw.setMobilePrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.MOBILE_PRICE));
        raw.setCataloguePrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.CATALOGUE_PRICE));
        raw.setImportantFeatures(ProductPayloadMapper.frozenFlag(frozen, ProductField.IMPORTANT_FEATURES));
        raw.setStock(ProductPayloadMapper.frozenFlag(frozen, ProductField.STOCK));
        raw.setStatus(ProductPayloadMapper.frozenFlag(frozen, ProductField.STATUS));
        raw.setDispatchTime(ProductPayloadMapper.frozenFlag(frozen, ProductField.DISPATCH_TIME));
        raw.setPackaging(ProductPayloadMapper.frozenFlag(frozen, ProductField.PACKAGING));
        raw.setInvoiceType(ProductPayloadMapper.frozenFlag(frozen, ProductField.INVOICE_TYPE));
        raw.setObligatoryIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.OBLIGATORY_IDENTIFIER));
        raw.setVoluntaryIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.VOLUNTARY_IDENTIFIER));
        raw.setReturnIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.RETURN_IDENTIFIER));
        raw.setDeliveryPriceList(ProductPayloadMapper.frozenFlag(frozen, ProductField.DELIVERY_PRICE_LIST));
        raw.setWeight(ProductPayloadMapper.frozenFlag(frozen, ProductField.WEIGHT));
        raw.setTaxRate(ProductPayloadMapper.frozenFlag(frozen, ProductField.TAX_RATE));
        return raw;
    }

    private static ProductUpdateFrozen updateFrozen(
            io.github.mgrtomaszzurawski.erli.domain.products.FrozenFields frozen) {
        ProductUpdateFrozen raw = new ProductUpdateFrozen();
        raw.setName(ProductPayloadMapper.frozenFlag(frozen, ProductField.NAME));
        raw.setDescription(ProductPayloadMapper.frozenFlag(frozen, ProductField.DESCRIPTION));
        raw.setEan(ProductPayloadMapper.frozenFlag(frozen, ProductField.EAN));
        raw.setSku(ProductPayloadMapper.frozenFlag(frozen, ProductField.SKU));
        raw.setExternalAttributes(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_ATTRIBUTES));
        raw.setExternalCategories(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_CATEGORIES));
        raw.setExternalVariantGroup(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_VARIANT_GROUP));
        raw.setImages(ProductPayloadMapper.frozenFlag(frozen, ProductField.IMAGES));
        raw.setFiles(ProductPayloadMapper.frozenFlag(frozen, ProductField.FILES));
        raw.setPrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.PRICE));
        raw.setMobilePrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.MOBILE_PRICE));
        raw.setCataloguePrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.CATALOGUE_PRICE));
        raw.setImportantFeatures(ProductPayloadMapper.frozenFlag(frozen, ProductField.IMPORTANT_FEATURES));
        raw.setStock(ProductPayloadMapper.frozenFlag(frozen, ProductField.STOCK));
        raw.setStatus(ProductPayloadMapper.frozenFlag(frozen, ProductField.STATUS));
        raw.setDispatchTime(ProductPayloadMapper.frozenFlag(frozen, ProductField.DISPATCH_TIME));
        raw.setPackaging(ProductPayloadMapper.frozenFlag(frozen, ProductField.PACKAGING));
        raw.setInvoiceType(ProductPayloadMapper.frozenFlag(frozen, ProductField.INVOICE_TYPE));
        raw.setObligatoryIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.OBLIGATORY_IDENTIFIER));
        raw.setVoluntaryIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.VOLUNTARY_IDENTIFIER));
        raw.setReturnIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.RETURN_IDENTIFIER));
        raw.setDeliveryPriceList(ProductPayloadMapper.frozenFlag(frozen, ProductField.DELIVERY_PRICE_LIST));
        raw.setWeight(ProductPayloadMapper.frozenFlag(frozen, ProductField.WEIGHT));
        raw.setTaxRate(ProductPayloadMapper.frozenFlag(frozen, ProductField.TAX_RATE));
        return raw;
    }

    /**
     * Clear the collection fields the generator pre-initialises to an empty list.
     *
     * <p>{@code ProductUpdate.images} and {@code ProductUpdate.externalReferences} are declared as
     * {@code = new ArrayList<>()}, so they serialize as {@code []} even when the caller never mentioned
     * them — and a {@code NON_NULL} inclusion rule does not suppress an empty list, only a null. On a
     * partial update Erli reads {@code "images": []} as "this product now has no images", so leaving the
     * generator's default in place would silently delete every image on any patch that did not set one.
     * Setting them to null keeps them off the wire entirely, which is what "not mentioned" must mean.
     */
    private static void dropUnsetCollections(ProductContent content, java.util.function.Consumer<Object> images,
            java.util.function.Consumer<Object> externalReferences) {
        if (content.images().isEmpty()) {
            images.accept(null);
        }
        if (content.externalReferences().isEmpty()) {
            externalReferences.accept(null);
        }
    }
}
