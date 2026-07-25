package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * A selectable product field. Both {@code GET /products/{externalId}} and {@code POST /products/_search}
 * accept a projection: pass the fields you actually need and the marketplace returns only those, which
 * keeps the (large) product payload small. Selecting nothing returns the full product.
 *
 * <p>A projection is always widened with the handful of fields a {@code Product} cannot be built
 * without, so a selection can never come back unmappable.
 */
public enum ProductField {

    /** The {@code name} field. */
    NAME("name"),

    /** The {@code description} field. */
    DESCRIPTION("description"),

    /** The {@code ean} field. */
    EAN("ean"),

    /** The {@code sku} field. */
    SKU("sku"),

    /** The {@code baseMarket} field. */
    BASE_MARKET("baseMarket"),

    /** The {@code externalReferences} field. */
    EXTERNAL_REFERENCES("externalReferences"),

    /** The {@code sourceFulfillmentProductId} field. */
    SOURCE_FULFILLMENT_PRODUCT_ID("sourceFulfillmentProductId"),

    /** The {@code importantFeatures} field. */
    IMPORTANT_FEATURES("importantFeatures"),

    /** The {@code externalAttributes} field. */
    EXTERNAL_ATTRIBUTES("externalAttributes"),

    /** The {@code externalCategories} field. */
    EXTERNAL_CATEGORIES("externalCategories"),

    /** The {@code externalVariantGroup} field. */
    EXTERNAL_VARIANT_GROUP("externalVariantGroup"),

    /** The {@code externalResponsibleProducer} field. */
    EXTERNAL_RESPONSIBLE_PRODUCER("externalResponsibleProducer"),

    /** The {@code externalResponsiblePerson} field. */
    EXTERNAL_RESPONSIBLE_PERSON("externalResponsiblePerson"),

    /** The {@code images} field. */
    IMAGES("images"),

    /** The {@code files} field. */
    FILES("files"),

    /** The {@code price} field. */
    PRICE("price"),

    /** The {@code mobilePrice} field. */
    MOBILE_PRICE("mobilePrice"),

    /** The {@code cataloguePrice} field. */
    CATALOGUE_PRICE("cataloguePrice"),

    /** The {@code referencePriceType} field. */
    REFERENCE_PRICE_TYPE("referencePriceType"),

    /** The {@code stock} field. */
    STOCK("stock"),

    /** The {@code status} field. */
    STATUS("status"),

    /** The {@code archived} field. */
    ARCHIVED("archived"),

    /** The {@code dispatchTime} field. */
    DISPATCH_TIME("dispatchTime"),

    /** The {@code deliveryPriceList} field. */
    DELIVERY_PRICE_LIST("deliveryPriceList"),

    /** The {@code weight} field. */
    WEIGHT("weight"),

    /** The {@code obligatoryIdentifier} field. */
    OBLIGATORY_IDENTIFIER("obligatoryIdentifier"),

    /** The {@code voluntaryIdentifier} field. */
    VOLUNTARY_IDENTIFIER("voluntaryIdentifier"),

    /** The {@code returnIdentifier} field. */
    RETURN_IDENTIFIER("returnIdentifier"),

    /** The {@code invoiceType} field. */
    INVOICE_TYPE("invoiceType"),

    /** The {@code taxRate} field. */
    TAX_RATE("taxRate"),

    /** The {@code basketLimit} field. */
    BASKET_LIMIT("basketLimit"),

    /** The {@code energyLabel} field. */
    ENERGY_LABEL("energyLabel"),

    /** The {@code instructionWithSafetyInformation} field. */
    INSTRUCTION_WITH_SAFETY_INFORMATION("instructionWithSafetyInformation"),

    /** The {@code informationCard} field. */
    INFORMATION_CARD("informationCard"),

    /** The {@code producerId} field. */
    PRODUCER_ID("producerId"),

    /** The {@code responsiblePersonId} field. */
    RESPONSIBLE_PERSON_ID("responsiblePersonId"),

    /** The {@code externalMetaProductId} field. */
    EXTERNAL_META_PRODUCT_ID("externalMetaProductId"),

    /** The {@code externalProductSets} field. */
    EXTERNAL_PRODUCT_SETS("externalProductSets"),

    /** The {@code productSets} field. */
    PRODUCT_SETS("productSets"),

    /** The {@code productAttachments} field. */
    PRODUCT_ATTACHMENTS("productAttachments"),

    /** The {@code automaticDiscountRuleId} field. */
    AUTOMATIC_DISCOUNT_RULE_ID("automaticDiscountRuleId"),

    /** The {@code markets} field. */
    MARKETS("markets"),

    /** The {@code translations} field. */
    TRANSLATIONS("translations"),

    /** The {@code externalId} field. */
    EXTERNAL_ID("externalId"),

    /** The {@code externalDescriptionHash} field. */
    EXTERNAL_DESCRIPTION_HASH("externalDescriptionHash"),

    /** The {@code externalDescription} field. */
    EXTERNAL_DESCRIPTION("externalDescription"),

    /** The {@code attributes} field. */
    ATTRIBUTES("attributes"),

    /** The {@code categories} field. */
    CATEGORIES("categories"),

    /** The {@code packaging} field. */
    PACKAGING("packaging"),

    /** The {@code marketplaceId} field. */
    MARKETPLACE_ID("marketplaceId"),

    /** The {@code slug} field. */
    SLUG("slug"),

    /** The {@code buyableProblems} field. */
    BUYABLE_PROBLEMS("buyableProblems"),

    /** The {@code archivedAt} field. */
    ARCHIVED_AT("archivedAt"),

    /** The {@code frozen} field. */
    FROZEN("frozen"),

    /** The {@code created} field. */
    CREATED("created"),

    /** The {@code updated} field. */
    UPDATED("updated"),

    /** The {@code producerIds} field. */
    PRODUCER_IDS("producerIds"),

    /** The {@code responsiblePersonIds} field. */
    RESPONSIBLE_PERSON_IDS("responsiblePersonIds");

    private final String wireName;

    ProductField(String wireName) {
        this.wireName = wireName;
    }

    /** The value the Erli API uses for this constant on the wire. */
    public String wireName() {
        return wireName;
    }
}
