package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A selectable product field. Both {@code GET /products/{externalId}} and {@code POST /products/_search}
 * accept a projection: pass the fields you actually need and the marketplace returns only those, which
 * keeps the (large) product payload small. Selecting nothing returns the full product.
 *
 * <p>The constant names are the SDK's; each maps to the wire name the API expects.
 */
public enum ProductField {

    /** The {@code name} field. */
    NAME,

    /** The {@code description} field. */
    DESCRIPTION,

    /** The {@code ean} field. */
    EAN,

    /** The {@code sku} field. */
    SKU,

    /** The {@code baseMarket} field. */
    BASE_MARKET,

    /** The {@code externalReferences} field. */
    EXTERNAL_REFERENCES,

    /** The {@code sourceFulfillmentProductId} field. */
    SOURCE_FULFILLMENT_PRODUCT_ID,

    /** The {@code importantFeatures} field. */
    IMPORTANT_FEATURES,

    /** The {@code externalAttributes} field. */
    EXTERNAL_ATTRIBUTES,

    /** The {@code externalCategories} field. */
    EXTERNAL_CATEGORIES,

    /** The {@code externalVariantGroup} field. */
    EXTERNAL_VARIANT_GROUP,

    /** The {@code externalResponsibleProducer} field. */
    EXTERNAL_RESPONSIBLE_PRODUCER,

    /** The {@code externalResponsiblePerson} field. */
    EXTERNAL_RESPONSIBLE_PERSON,

    /** The {@code images} field. */
    IMAGES,

    /** The {@code files} field. */
    FILES,

    /** The {@code price} field. */
    PRICE,

    /** The {@code mobilePrice} field. */
    MOBILE_PRICE,

    /** The {@code cataloguePrice} field. */
    CATALOGUE_PRICE,

    /** The {@code referencePriceType} field. */
    REFERENCE_PRICE_TYPE,

    /** The {@code stock} field. */
    STOCK,

    /** The {@code status} field. */
    STATUS,

    /** The {@code archived} field. */
    ARCHIVED,

    /** The {@code dispatchTime} field. */
    DISPATCH_TIME,

    /** The {@code deliveryPriceList} field. */
    DELIVERY_PRICE_LIST,

    /** The {@code weight} field. */
    WEIGHT,

    /** The {@code obligatoryIdentifier} field. */
    OBLIGATORY_IDENTIFIER,

    /** The {@code voluntaryIdentifier} field. */
    VOLUNTARY_IDENTIFIER,

    /** The {@code returnIdentifier} field. */
    RETURN_IDENTIFIER,

    /** The {@code invoiceType} field. */
    INVOICE_TYPE,

    /** The {@code taxRate} field. */
    TAX_RATE,

    /** The {@code basketLimit} field. */
    BASKET_LIMIT,

    /** The {@code energyLabel} field. */
    ENERGY_LABEL,

    /** The {@code instructionWithSafetyInformation} field. */
    INSTRUCTION_WITH_SAFETY_INFORMATION,

    /** The {@code informationCard} field. */
    INFORMATION_CARD,

    /** The {@code producerId} field. */
    PRODUCER_ID,

    /** The {@code responsiblePersonId} field. */
    RESPONSIBLE_PERSON_ID,

    /** The {@code externalMetaProductId} field. */
    EXTERNAL_META_PRODUCT_ID,

    /** The {@code externalProductSets} field. */
    EXTERNAL_PRODUCT_SETS,

    /** The {@code productSets} field. */
    PRODUCT_SETS,

    /** The {@code productAttachments} field. */
    PRODUCT_ATTACHMENTS,

    /** The {@code automaticDiscountRuleId} field. */
    AUTOMATIC_DISCOUNT_RULE_ID,

    /** The {@code markets} field. */
    MARKETS,

    /** The {@code translations} field. */
    TRANSLATIONS,

    /** The {@code externalId} field. */
    EXTERNAL_ID,

    /** The {@code externalDescriptionHash} field. */
    EXTERNAL_DESCRIPTION_HASH,

    /** The {@code externalDescription} field. */
    EXTERNAL_DESCRIPTION,

    /** The {@code attributes} field. */
    ATTRIBUTES,

    /** The {@code categories} field. */
    CATEGORIES,

    /** The {@code packaging} field. */
    PACKAGING,

    /** The {@code marketplaceId} field. */
    MARKETPLACE_ID,

    /** The {@code slug} field. */
    SLUG,

    /** The {@code buyableProblems} field. */
    BUYABLE_PROBLEMS,

    /** The {@code archivedAt} field. */
    ARCHIVED_AT,

    /** The {@code frozen} field. */
    FROZEN,

    /** The {@code created} field. */
    CREATED,

    /** The {@code updated} field. */
    UPDATED,

    /** The {@code producerIds} field. */
    PRODUCER_IDS,

    /** The {@code responsiblePersonIds} field. */
    RESPONSIBLE_PERSON_IDS;
}
