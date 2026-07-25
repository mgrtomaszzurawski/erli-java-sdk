package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.domain.products.ProductContent;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDraft;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductPatch;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreate;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductUpdate;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductUpdateFrozen;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductsBatchUpdatePatchRequestInner;
import org.openapitools.jackson.nullable.JsonNullable;

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
        ProductCreate rawRequest = new ProductCreate();
        ProductContent content = draft.content();
        content.name().ifPresent(rawRequest::setName);
        content.description().ifPresent(value -> rawRequest.setDescription(ProductPayloadMapper.description(value)));
        content.ean().ifPresent(rawRequest::setEan);
        content.sku().ifPresent(rawRequest::setSku);
        content.baseMarket().ifPresent(value -> rawRequest.setBaseMarket(ProductCreate.BaseMarketEnum.fromValue(value.wireName())));
        content.externalReferences().ifPresent(value -> rawRequest.setExternalReferences(ProductPayloadMapper.externalReferences(value)));
        content.sourceFulfillmentProductId().ifPresent(rawRequest::setSourceFulfillmentProductId);
        content.importantFeatures().ifPresent(rawRequest::setImportantFeatures);
        content.externalAttributes().ifPresent(value -> rawRequest.setExternalAttributes(ProductPayloadMapper.externalAttributes(value)));
        content.externalCategories().ifPresent(value -> rawRequest.setExternalCategories(ProductPayloadMapper.externalCategories(value)));
        content.externalVariantGroup().ifPresent(value -> rawRequest.setExternalVariantGroup(ProductPayloadMapper.externalVariantGroup(value)));
        content.externalResponsibleProducer().ifPresent(value -> rawRequest.setExternalResponsibleProducer(ProductPayloadMapper.responsibleProducers(value)));
        content.externalResponsiblePerson().ifPresent(value -> rawRequest.setExternalResponsiblePerson(ProductPayloadMapper.responsiblePersons(value)));
        content.images().ifPresent(value -> rawRequest.setImages(ProductPayloadMapper.images(value)));
        content.files().ifPresent(value -> rawRequest.setFiles(ProductPayloadMapper.files(value)));
        content.price().ifPresent(value -> rawRequest.setPrice(ProductValues.toMinorUnits(value)));
        content.mobilePrice().ifPresent(value -> rawRequest.setMobilePrice(ProductValues.toMinorUnits(value)));
        content.cataloguePrice().ifPresent(value -> rawRequest.setCataloguePrice(ProductValues.toMinorUnits(value)));
        content.referencePriceType().ifPresent(value -> rawRequest.setReferencePriceType(ProductCreate.ReferencePriceTypeEnum.fromValue(value.wireName())));
        content.stock().ifPresent(rawRequest::setStock);
        content.status().ifPresent(value -> rawRequest.setStatus(ProductCreate.StatusEnum.fromValue(value.wireName())));
        content.archived().ifPresent(rawRequest::setArchived);
        content.dispatchTime().ifPresent(value -> rawRequest.setDispatchTime(ProductPayloadMapper.dispatchTime(value)));
        content.deliveryPriceList().ifPresent(rawRequest::setDeliveryPriceList);
        content.weight().ifPresent(rawRequest::setWeight);
        content.obligatoryIdentifier().ifPresent(rawRequest::setObligatoryIdentifier);
        content.voluntaryIdentifier().ifPresent(rawRequest::setVoluntaryIdentifier);
        content.returnIdentifier().ifPresent(rawRequest::setReturnIdentifier);
        content.invoiceType().ifPresent(value -> rawRequest.setInvoiceType(ProductCreate.InvoiceTypeEnum.fromValue(value.wireName())));
        content.taxRate().ifPresent(value -> rawRequest.setTaxRate(ProductCreate.TaxRateEnum.fromValue(value.wireName())));
        content.basketLimit().ifPresent(rawRequest::setBasketLimit);
        content.energyLabel().ifPresent(rawRequest::setEnergyLabel);
        content.instructionWithSafetyInformation().ifPresent(rawRequest::setInstructionWithSafetyInformation);
        content.informationCard().ifPresent(rawRequest::setInformationCard);
        content.producerId().ifPresent(rawRequest::setProducerId);
        content.responsiblePersonId().ifPresent(rawRequest::setResponsiblePersonId);
        content.externalMetaProductId().ifPresent(rawRequest::setExternalMetaProductId);
        content.externalProductSets().ifPresent(value -> rawRequest.setExternalProductSets(ProductPayloadMapper.externalProductSets(value)));
        content.productSets().ifPresent(value -> rawRequest.setProductSets(ProductPayloadMapper.productSets(value)));
        content.productAttachments().ifPresent(value -> rawRequest.setProductAttachments(ProductPayloadMapper.productAttachments(value)));
        content.automaticDiscountRuleId().ifPresent(rawRequest::setAutomaticDiscountRuleId);
        content.packaging().ifPresent(value -> rawRequest.setPackaging(ProductPayloadMapper.packaging(value)));
        content.frozen().ifPresent(frozen -> rawRequest.setFrozen(createFrozen(frozen)));
        dropUnsetCollections(content, value -> rawRequest.setImages(null), value -> rawRequest.setExternalReferences(null));
        return rawRequest;
    }

    /** The update payload for {@code PATCH /products/{externalId}}. */
    static ProductUpdate toUpdate(ProductPatch patch) {
        ProductUpdate rawRequest = new ProductUpdate();
        ProductContent content = patch.content();
        content.name().ifPresent(rawRequest::setName);
        content.description().ifPresent(value -> rawRequest.setDescription(ProductPayloadMapper.description(value)));
        content.ean().ifPresent(rawRequest::setEan);
        content.sku().ifPresent(rawRequest::setSku);
        content.baseMarket().ifPresent(value -> rawRequest.setBaseMarket(ProductUpdate.BaseMarketEnum.fromValue(value.wireName())));
        content.externalReferences().ifPresent(value -> rawRequest.setExternalReferences(ProductPayloadMapper.externalReferences(value)));
        content.sourceFulfillmentProductId().ifPresent(rawRequest::setSourceFulfillmentProductId);
        content.importantFeatures().ifPresent(rawRequest::setImportantFeatures);
        content.externalAttributes().ifPresent(value -> rawRequest.setExternalAttributes(ProductPayloadMapper.externalAttributes(value)));
        content.externalCategories().ifPresent(value -> rawRequest.setExternalCategories(ProductPayloadMapper.externalCategories(value)));
        content.externalVariantGroup().ifPresent(value -> rawRequest.setExternalVariantGroup(ProductPayloadMapper.externalVariantGroup(value)));
        content.externalResponsibleProducer().ifPresent(value -> rawRequest.setExternalResponsibleProducer(ProductPayloadMapper.responsibleProducers(value)));
        content.externalResponsiblePerson().ifPresent(value -> rawRequest.setExternalResponsiblePerson(ProductPayloadMapper.responsiblePersons(value)));
        content.images().ifPresent(value -> rawRequest.setImages(ProductPayloadMapper.images(value)));
        content.files().ifPresent(value -> rawRequest.setFiles(ProductPayloadMapper.files(value)));
        content.price().ifPresent(value -> rawRequest.setPrice(ProductValues.toMinorUnits(value)));
        content.mobilePrice().ifPresent(value -> rawRequest.setMobilePrice(ProductValues.toMinorUnits(value)));
        content.cataloguePrice().ifPresent(value -> rawRequest.setCataloguePrice(ProductValues.toMinorUnits(value)));
        content.referencePriceType().ifPresent(value -> rawRequest.setReferencePriceType(ProductUpdate.ReferencePriceTypeEnum.fromValue(value.wireName())));
        content.stock().ifPresent(rawRequest::setStock);
        content.status().ifPresent(value -> rawRequest.setStatus(ProductUpdate.StatusEnum.fromValue(value.wireName())));
        content.archived().ifPresent(rawRequest::setArchived);
        content.dispatchTime().ifPresent(value -> rawRequest.setDispatchTime(ProductPayloadMapper.dispatchTime(value)));
        content.deliveryPriceList().ifPresent(rawRequest::setDeliveryPriceList);
        content.weight().ifPresent(rawRequest::setWeight);
        content.obligatoryIdentifier().ifPresent(rawRequest::setObligatoryIdentifier);
        content.voluntaryIdentifier().ifPresent(rawRequest::setVoluntaryIdentifier);
        content.returnIdentifier().ifPresent(rawRequest::setReturnIdentifier);
        content.invoiceType().ifPresent(value -> rawRequest.setInvoiceType(ProductUpdate.InvoiceTypeEnum.fromValue(value.wireName())));
        content.taxRate().ifPresent(value -> rawRequest.setTaxRate(ProductUpdate.TaxRateEnum.fromValue(value.wireName())));
        content.basketLimit().ifPresent(rawRequest::setBasketLimit);
        content.energyLabel().ifPresent(rawRequest::setEnergyLabel);
        content.instructionWithSafetyInformation().ifPresent(rawRequest::setInstructionWithSafetyInformation);
        content.informationCard().ifPresent(rawRequest::setInformationCard);
        content.producerId().ifPresent(rawRequest::setProducerId);
        content.responsiblePersonId().ifPresent(rawRequest::setResponsiblePersonId);
        content.externalMetaProductId().ifPresent(rawRequest::setExternalMetaProductId);
        content.externalProductSets().ifPresent(value -> rawRequest.setExternalProductSets(ProductPayloadMapper.externalProductSets(value)));
        content.productSets().ifPresent(value -> rawRequest.setProductSets(ProductPayloadMapper.productSets(value)));
        content.productAttachments().ifPresent(value -> rawRequest.setProductAttachments(ProductPayloadMapper.productAttachments(value)));
        content.automaticDiscountRuleId().ifPresent(rawRequest::setAutomaticDiscountRuleId);
        content.packaging().ifPresent(value -> rawRequest.setPackaging(ProductPayloadMapper.packaging(value)));
        content.frozen().ifPresent(frozen -> rawRequest.setFrozen(updateFrozen(frozen)));
        patch.newExternalId().ifPresent(value -> rawRequest.setNewExternalId(value.value()));
        applyOverrideFrozen(patch, rawRequest::setOverrideFrozen, rawRequest::setOverrideFrozen_JsonNullable);
        dropUnsetCollections(content, value -> rawRequest.setImages(null), value -> rawRequest.setExternalReferences(null));
        for (ProductField field : patch.cleared()) {
            clear(rawRequest, field);
        }
        return rawRequest;
    }

    /**
     * One entry of {@code PATCH /products/batch-update}: the same update payload plus the id it applies
     * to. The generated batch item is a flattened copy of {@code ProductUpdate} rather than a wrapper, so
     * the update is built once and copied across field by field.
     */
    static ProductsBatchUpdatePatchRequestInner toBatchEntry(
            io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId externalId, ProductPatch patch) {
        ProductsBatchUpdatePatchRequestInner rawRequest = new ProductsBatchUpdatePatchRequestInner();
        ProductContent content = patch.content();
        content.name().ifPresent(rawRequest::setName);
        content.description().ifPresent(value -> rawRequest.setDescription(ProductPayloadMapper.description(value)));
        content.ean().ifPresent(rawRequest::setEan);
        content.sku().ifPresent(rawRequest::setSku);
        content.baseMarket().ifPresent(value -> rawRequest.setBaseMarket(ProductsBatchUpdatePatchRequestInner.BaseMarketEnum.fromValue(value.wireName())));
        content.externalReferences().ifPresent(value -> rawRequest.setExternalReferences(ProductPayloadMapper.externalReferences(value)));
        content.sourceFulfillmentProductId().ifPresent(rawRequest::setSourceFulfillmentProductId);
        content.importantFeatures().ifPresent(rawRequest::setImportantFeatures);
        content.externalAttributes().ifPresent(value -> rawRequest.setExternalAttributes(ProductPayloadMapper.externalAttributes(value)));
        content.externalCategories().ifPresent(value -> rawRequest.setExternalCategories(ProductPayloadMapper.externalCategories(value)));
        content.externalVariantGroup().ifPresent(value -> rawRequest.setExternalVariantGroup(ProductPayloadMapper.externalVariantGroup(value)));
        content.externalResponsibleProducer().ifPresent(value -> rawRequest.setExternalResponsibleProducer(ProductPayloadMapper.responsibleProducers(value)));
        content.externalResponsiblePerson().ifPresent(value -> rawRequest.setExternalResponsiblePerson(ProductPayloadMapper.responsiblePersons(value)));
        content.images().ifPresent(value -> rawRequest.setImages(ProductPayloadMapper.images(value)));
        content.files().ifPresent(value -> rawRequest.setFiles(ProductPayloadMapper.files(value)));
        content.price().ifPresent(value -> rawRequest.setPrice(ProductValues.toMinorUnits(value)));
        content.mobilePrice().ifPresent(value -> rawRequest.setMobilePrice(ProductValues.toMinorUnits(value)));
        content.cataloguePrice().ifPresent(value -> rawRequest.setCataloguePrice(ProductValues.toMinorUnits(value)));
        content.referencePriceType().ifPresent(value -> rawRequest.setReferencePriceType(ProductsBatchUpdatePatchRequestInner.ReferencePriceTypeEnum.fromValue(value.wireName())));
        content.stock().ifPresent(rawRequest::setStock);
        content.status().ifPresent(value -> rawRequest.setStatus(ProductsBatchUpdatePatchRequestInner.StatusEnum.fromValue(value.wireName())));
        content.archived().ifPresent(rawRequest::setArchived);
        content.dispatchTime().ifPresent(value -> rawRequest.setDispatchTime(ProductPayloadMapper.dispatchTime(value)));
        content.deliveryPriceList().ifPresent(rawRequest::setDeliveryPriceList);
        content.weight().ifPresent(rawRequest::setWeight);
        content.obligatoryIdentifier().ifPresent(rawRequest::setObligatoryIdentifier);
        content.voluntaryIdentifier().ifPresent(rawRequest::setVoluntaryIdentifier);
        content.returnIdentifier().ifPresent(rawRequest::setReturnIdentifier);
        content.invoiceType().ifPresent(value -> rawRequest.setInvoiceType(ProductsBatchUpdatePatchRequestInner.InvoiceTypeEnum.fromValue(value.wireName())));
        content.taxRate().ifPresent(value -> rawRequest.setTaxRate(ProductsBatchUpdatePatchRequestInner.TaxRateEnum.fromValue(value.wireName())));
        content.basketLimit().ifPresent(rawRequest::setBasketLimit);
        content.energyLabel().ifPresent(rawRequest::setEnergyLabel);
        content.instructionWithSafetyInformation().ifPresent(rawRequest::setInstructionWithSafetyInformation);
        content.informationCard().ifPresent(rawRequest::setInformationCard);
        content.producerId().ifPresent(rawRequest::setProducerId);
        content.responsiblePersonId().ifPresent(rawRequest::setResponsiblePersonId);
        content.externalMetaProductId().ifPresent(rawRequest::setExternalMetaProductId);
        content.externalProductSets().ifPresent(value -> rawRequest.setExternalProductSets(ProductPayloadMapper.externalProductSets(value)));
        content.productSets().ifPresent(value -> rawRequest.setProductSets(ProductPayloadMapper.productSets(value)));
        content.productAttachments().ifPresent(value -> rawRequest.setProductAttachments(ProductPayloadMapper.productAttachments(value)));
        content.automaticDiscountRuleId().ifPresent(rawRequest::setAutomaticDiscountRuleId);
        content.packaging().ifPresent(value -> rawRequest.setPackaging(ProductPayloadMapper.packaging(value)));
        content.frozen().ifPresent(frozen -> rawRequest.setFrozen(updateFrozen(frozen)));
        patch.newExternalId().ifPresent(value -> rawRequest.setNewExternalId(value.value()));
        applyOverrideFrozen(patch, rawRequest::setOverrideFrozen, rawRequest::setOverrideFrozen_JsonNullable);
        dropUnsetCollections(content, value -> rawRequest.setImages(null), value -> rawRequest.setExternalReferences(null));
        for (ProductField field : patch.cleared()) {
            clearBatchEntry(rawRequest, field);
        }
        rawRequest.setExternalId(externalId.value());
        return rawRequest;
    }

    /**
     * Send an explicit null for a cleared field. The switch covers exactly
     * {@link ProductPatch#clearableFields()}; anything else was already rejected when the patch was
     * built, so reaching the default here means those two lists have drifted apart.
     */
    private static void clear(ProductUpdate rawRequest, ProductField field) {
        switch (field) {
            case DESCRIPTION -> rawRequest.setDescription(null);
            case EAN -> rawRequest.setEan(null);
            case SKU -> rawRequest.setSku(null);
            case BASE_MARKET -> rawRequest.setBaseMarket(null);
            case SOURCE_FULFILLMENT_PRODUCT_ID -> rawRequest.setSourceFulfillmentProductId(null);
            case IMPORTANT_FEATURES -> rawRequest.setImportantFeatures(null);
            case EXTERNAL_ATTRIBUTES -> rawRequest.setExternalAttributes(null);
            case EXTERNAL_CATEGORIES -> rawRequest.setExternalCategories(null);
            case EXTERNAL_VARIANT_GROUP -> rawRequest.setExternalVariantGroup(null);
            case EXTERNAL_RESPONSIBLE_PRODUCER -> rawRequest.setExternalResponsibleProducer(null);
            case EXTERNAL_RESPONSIBLE_PERSON -> rawRequest.setExternalResponsiblePerson(null);
            case FILES -> rawRequest.setFiles(null);
            case MOBILE_PRICE -> rawRequest.setMobilePrice(null);
            case CATALOGUE_PRICE -> rawRequest.setCataloguePrice(null);
            case REFERENCE_PRICE_TYPE -> rawRequest.setReferencePriceType(null);
            case STATUS -> rawRequest.setStatus(null);
            case DELIVERY_PRICE_LIST -> rawRequest.setDeliveryPriceList(null);
            case WEIGHT -> rawRequest.setWeight(null);
            case OBLIGATORY_IDENTIFIER -> rawRequest.setObligatoryIdentifier(null);
            case VOLUNTARY_IDENTIFIER -> rawRequest.setVoluntaryIdentifier(null);
            case RETURN_IDENTIFIER -> rawRequest.setReturnIdentifier(null);
            case INVOICE_TYPE -> rawRequest.setInvoiceType(null);
            case TAX_RATE -> rawRequest.setTaxRate(null);
            case BASKET_LIMIT -> rawRequest.setBasketLimit(null);
            case ENERGY_LABEL -> rawRequest.setEnergyLabel(null);
            case INSTRUCTION_WITH_SAFETY_INFORMATION -> rawRequest.setInstructionWithSafetyInformation(null);
            case INFORMATION_CARD -> rawRequest.setInformationCard(null);
            case EXTERNAL_META_PRODUCT_ID -> rawRequest.setExternalMetaProductId(null);
            case EXTERNAL_PRODUCT_SETS -> rawRequest.setExternalProductSets(null);
            case PRODUCT_SETS -> rawRequest.setProductSets(null);
            case PRODUCT_ATTACHMENTS -> rawRequest.setProductAttachments(null);
            case AUTOMATIC_DISCOUNT_RULE_ID -> rawRequest.setAutomaticDiscountRuleId(null);
            default -> throw new IllegalStateException(
                    "Field '" + field + "' is not clearable but reached the update mapper");
        }
    }

    private static void clearBatchEntry(ProductsBatchUpdatePatchRequestInner rawRequest, ProductField field) {
        switch (field) {
            case DESCRIPTION -> rawRequest.setDescription(null);
            case EAN -> rawRequest.setEan(null);
            case SKU -> rawRequest.setSku(null);
            case BASE_MARKET -> rawRequest.setBaseMarket(null);
            case SOURCE_FULFILLMENT_PRODUCT_ID -> rawRequest.setSourceFulfillmentProductId(null);
            case IMPORTANT_FEATURES -> rawRequest.setImportantFeatures(null);
            case EXTERNAL_ATTRIBUTES -> rawRequest.setExternalAttributes(null);
            case EXTERNAL_CATEGORIES -> rawRequest.setExternalCategories(null);
            case EXTERNAL_VARIANT_GROUP -> rawRequest.setExternalVariantGroup(null);
            case EXTERNAL_RESPONSIBLE_PRODUCER -> rawRequest.setExternalResponsibleProducer(null);
            case EXTERNAL_RESPONSIBLE_PERSON -> rawRequest.setExternalResponsiblePerson(null);
            case FILES -> rawRequest.setFiles(null);
            case MOBILE_PRICE -> rawRequest.setMobilePrice(null);
            case CATALOGUE_PRICE -> rawRequest.setCataloguePrice(null);
            case REFERENCE_PRICE_TYPE -> rawRequest.setReferencePriceType(null);
            case STATUS -> rawRequest.setStatus(null);
            case DELIVERY_PRICE_LIST -> rawRequest.setDeliveryPriceList(null);
            case WEIGHT -> rawRequest.setWeight(null);
            case OBLIGATORY_IDENTIFIER -> rawRequest.setObligatoryIdentifier(null);
            case VOLUNTARY_IDENTIFIER -> rawRequest.setVoluntaryIdentifier(null);
            case RETURN_IDENTIFIER -> rawRequest.setReturnIdentifier(null);
            case INVOICE_TYPE -> rawRequest.setInvoiceType(null);
            case TAX_RATE -> rawRequest.setTaxRate(null);
            case BASKET_LIMIT -> rawRequest.setBasketLimit(null);
            case ENERGY_LABEL -> rawRequest.setEnergyLabel(null);
            case INSTRUCTION_WITH_SAFETY_INFORMATION -> rawRequest.setInstructionWithSafetyInformation(null);
            case INFORMATION_CARD -> rawRequest.setInformationCard(null);
            case EXTERNAL_META_PRODUCT_ID -> rawRequest.setExternalMetaProductId(null);
            case EXTERNAL_PRODUCT_SETS -> rawRequest.setExternalProductSets(null);
            case PRODUCT_SETS -> rawRequest.setProductSets(null);
            case PRODUCT_ATTACHMENTS -> rawRequest.setProductAttachments(null);
            case AUTOMATIC_DISCOUNT_RULE_ID -> rawRequest.setAutomaticDiscountRuleId(null);
            default -> throw new IllegalStateException(
                    "Field '" + field + "' is not clearable but reached the batch mapper");
        }
    }

    private static io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateFrozen createFrozen(
            io.github.mgrtomaszzurawski.erli.domain.products.FrozenFields frozen) {
        var rawRequest = new io.github.mgrtomaszzurawski.erli.rest.model.ProductCreateFrozen();
        rawRequest.setName(ProductPayloadMapper.frozenFlag(frozen, ProductField.NAME));
        rawRequest.setDescription(ProductPayloadMapper.frozenFlag(frozen, ProductField.DESCRIPTION));
        rawRequest.setEan(ProductPayloadMapper.frozenFlag(frozen, ProductField.EAN));
        rawRequest.setSku(ProductPayloadMapper.frozenFlag(frozen, ProductField.SKU));
        rawRequest.setExternalAttributes(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_ATTRIBUTES));
        rawRequest.setExternalCategories(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_CATEGORIES));
        rawRequest.setExternalVariantGroup(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_VARIANT_GROUP));
        rawRequest.setImages(ProductPayloadMapper.frozenFlag(frozen, ProductField.IMAGES));
        rawRequest.setFiles(ProductPayloadMapper.frozenFlag(frozen, ProductField.FILES));
        rawRequest.setPrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.PRICE));
        rawRequest.setMobilePrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.MOBILE_PRICE));
        rawRequest.setCataloguePrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.CATALOGUE_PRICE));
        rawRequest.setImportantFeatures(ProductPayloadMapper.frozenFlag(frozen, ProductField.IMPORTANT_FEATURES));
        rawRequest.setStock(ProductPayloadMapper.frozenFlag(frozen, ProductField.STOCK));
        rawRequest.setStatus(ProductPayloadMapper.frozenFlag(frozen, ProductField.STATUS));
        rawRequest.setDispatchTime(ProductPayloadMapper.frozenFlag(frozen, ProductField.DISPATCH_TIME));
        rawRequest.setPackaging(ProductPayloadMapper.frozenFlag(frozen, ProductField.PACKAGING));
        rawRequest.setInvoiceType(ProductPayloadMapper.frozenFlag(frozen, ProductField.INVOICE_TYPE));
        rawRequest.setObligatoryIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.OBLIGATORY_IDENTIFIER));
        rawRequest.setVoluntaryIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.VOLUNTARY_IDENTIFIER));
        rawRequest.setReturnIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.RETURN_IDENTIFIER));
        rawRequest.setDeliveryPriceList(ProductPayloadMapper.frozenFlag(frozen, ProductField.DELIVERY_PRICE_LIST));
        rawRequest.setWeight(ProductPayloadMapper.frozenFlag(frozen, ProductField.WEIGHT));
        rawRequest.setTaxRate(ProductPayloadMapper.frozenFlag(frozen, ProductField.TAX_RATE));
        return rawRequest;
    }

    private static ProductUpdateFrozen updateFrozen(
            io.github.mgrtomaszzurawski.erli.domain.products.FrozenFields frozen) {
        ProductUpdateFrozen rawRequest = new ProductUpdateFrozen();
        rawRequest.setName(ProductPayloadMapper.frozenFlag(frozen, ProductField.NAME));
        rawRequest.setDescription(ProductPayloadMapper.frozenFlag(frozen, ProductField.DESCRIPTION));
        rawRequest.setEan(ProductPayloadMapper.frozenFlag(frozen, ProductField.EAN));
        rawRequest.setSku(ProductPayloadMapper.frozenFlag(frozen, ProductField.SKU));
        rawRequest.setExternalAttributes(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_ATTRIBUTES));
        rawRequest.setExternalCategories(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_CATEGORIES));
        rawRequest.setExternalVariantGroup(ProductPayloadMapper.frozenFlag(frozen, ProductField.EXTERNAL_VARIANT_GROUP));
        rawRequest.setImages(ProductPayloadMapper.frozenFlag(frozen, ProductField.IMAGES));
        rawRequest.setFiles(ProductPayloadMapper.frozenFlag(frozen, ProductField.FILES));
        rawRequest.setPrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.PRICE));
        rawRequest.setMobilePrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.MOBILE_PRICE));
        rawRequest.setCataloguePrice(ProductPayloadMapper.frozenFlag(frozen, ProductField.CATALOGUE_PRICE));
        rawRequest.setImportantFeatures(ProductPayloadMapper.frozenFlag(frozen, ProductField.IMPORTANT_FEATURES));
        rawRequest.setStock(ProductPayloadMapper.frozenFlag(frozen, ProductField.STOCK));
        rawRequest.setStatus(ProductPayloadMapper.frozenFlag(frozen, ProductField.STATUS));
        rawRequest.setDispatchTime(ProductPayloadMapper.frozenFlag(frozen, ProductField.DISPATCH_TIME));
        rawRequest.setPackaging(ProductPayloadMapper.frozenFlag(frozen, ProductField.PACKAGING));
        rawRequest.setInvoiceType(ProductPayloadMapper.frozenFlag(frozen, ProductField.INVOICE_TYPE));
        rawRequest.setObligatoryIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.OBLIGATORY_IDENTIFIER));
        rawRequest.setVoluntaryIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.VOLUNTARY_IDENTIFIER));
        rawRequest.setReturnIdentifier(ProductPayloadMapper.frozenFlag(frozen, ProductField.RETURN_IDENTIFIER));
        rawRequest.setDeliveryPriceList(ProductPayloadMapper.frozenFlag(frozen, ProductField.DELIVERY_PRICE_LIST));
        rawRequest.setWeight(ProductPayloadMapper.frozenFlag(frozen, ProductField.WEIGHT));
        rawRequest.setTaxRate(ProductPayloadMapper.frozenFlag(frozen, ProductField.TAX_RATE));
        return rawRequest;
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

    /**
     * Set {@code overrideFrozen}, or remove it from the payload entirely when the caller did not ask
     * for it.
     *
     * <p>The generator declares this one property as {@code JsonNullable.<Object>of(null)} — an
     * <em>explicit null</em> — where every other nullable property defaults to {@code undefined()}.
     * Left alone it therefore travels on every single update, and the marketplace rejects it:
     * {@code 400 "overrideFrozen must be [true]"}. Resetting it to {@code undefined()} is what makes an
     * ordinary patch a legal request. Observed live against the sandbox, 2026-07-25.
     */
    private static void applyOverrideFrozen(ProductPatch patch, java.util.function.Consumer<Object> setValue,
            java.util.function.Consumer<JsonNullable<Object>> setRaw) {
        if (patch.overrideFrozen()) {
            setValue.accept(Boolean.TRUE);
        } else {
            setRaw.accept(JsonNullable.undefined());
        }
    }
}
