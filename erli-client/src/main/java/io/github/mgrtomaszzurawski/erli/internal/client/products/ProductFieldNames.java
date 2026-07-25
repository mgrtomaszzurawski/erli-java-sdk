package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;

/**
 * Translates {@link ProductField} constants to the wire names the marketplace expects in a {@code fields}
 * projection, and back again for the {@code updatedFields} and {@code frozen} payloads.
 *
 * <p>The mapping is an explicit switch rather than a name transformation: it decouples the SDK's constant
 * names from the API's spelling, and the exhaustive switch turns a future spec addition into a compile
 * error here instead of a runtime surprise. Internal: never exported.
 */
final class ProductFieldNames {

    private ProductFieldNames() {
    }

    /** The wire name of a field. */
    static String wireName(ProductField field) {
        return switch (field) {
            case NAME -> "name";
            case DESCRIPTION -> "description";
            case EAN -> "ean";
            case SKU -> "sku";
            case BASE_MARKET -> "baseMarket";
            case EXTERNAL_REFERENCES -> "externalReferences";
            case SOURCE_FULFILLMENT_PRODUCT_ID -> "sourceFulfillmentProductId";
            case IMPORTANT_FEATURES -> "importantFeatures";
            case EXTERNAL_ATTRIBUTES -> "externalAttributes";
            case EXTERNAL_CATEGORIES -> "externalCategories";
            case EXTERNAL_VARIANT_GROUP -> "externalVariantGroup";
            case EXTERNAL_RESPONSIBLE_PRODUCER -> "externalResponsibleProducer";
            case EXTERNAL_RESPONSIBLE_PERSON -> "externalResponsiblePerson";
            case IMAGES -> "images";
            case FILES -> "files";
            case PRICE -> "price";
            case MOBILE_PRICE -> "mobilePrice";
            case CATALOGUE_PRICE -> "cataloguePrice";
            case REFERENCE_PRICE_TYPE -> "referencePriceType";
            case STOCK -> "stock";
            case STATUS -> "status";
            case ARCHIVED -> "archived";
            case DISPATCH_TIME -> "dispatchTime";
            case DELIVERY_PRICE_LIST -> "deliveryPriceList";
            case WEIGHT -> "weight";
            case OBLIGATORY_IDENTIFIER -> "obligatoryIdentifier";
            case VOLUNTARY_IDENTIFIER -> "voluntaryIdentifier";
            case RETURN_IDENTIFIER -> "returnIdentifier";
            case INVOICE_TYPE -> "invoiceType";
            case TAX_RATE -> "taxRate";
            case BASKET_LIMIT -> "basketLimit";
            case ENERGY_LABEL -> "energyLabel";
            case INSTRUCTION_WITH_SAFETY_INFORMATION -> "instructionWithSafetyInformation";
            case INFORMATION_CARD -> "informationCard";
            case PRODUCER_ID -> "producerId";
            case RESPONSIBLE_PERSON_ID -> "responsiblePersonId";
            case EXTERNAL_META_PRODUCT_ID -> "externalMetaProductId";
            case EXTERNAL_PRODUCT_SETS -> "externalProductSets";
            case PRODUCT_SETS -> "productSets";
            case PRODUCT_ATTACHMENTS -> "productAttachments";
            case AUTOMATIC_DISCOUNT_RULE_ID -> "automaticDiscountRuleId";
            case MARKETS -> "markets";
            case TRANSLATIONS -> "translations";
            case EXTERNAL_ID -> "externalId";
            case EXTERNAL_DESCRIPTION_HASH -> "externalDescriptionHash";
            case EXTERNAL_DESCRIPTION -> "externalDescription";
            case ATTRIBUTES -> "attributes";
            case CATEGORIES -> "categories";
            case PACKAGING -> "packaging";
            case MARKETPLACE_ID -> "marketplaceId";
            case SLUG -> "slug";
            case BUYABLE_PROBLEMS -> "buyableProblems";
            case ARCHIVED_AT -> "archivedAt";
            case FROZEN -> "frozen";
            case CREATED -> "created";
            case UPDATED -> "updated";
            case PRODUCER_IDS -> "producerIds";
            case RESPONSIBLE_PERSON_IDS -> "responsiblePersonIds";
        };
    }

    /**
     * The field a wire name denotes, or {@code null} when the marketplace names a field this SDK version
     * does not know. Unknown names are skipped rather than fatal: the API may add fields at any time, and
     * a reader must not break because a newer field appeared in {@code frozen} or {@code updatedFields}.
     */
    static ProductField fromWireName(String wireName) {
        return switch (wireName) {
            case "name" -> ProductField.NAME;
            case "description" -> ProductField.DESCRIPTION;
            case "ean" -> ProductField.EAN;
            case "sku" -> ProductField.SKU;
            case "baseMarket" -> ProductField.BASE_MARKET;
            case "externalReferences" -> ProductField.EXTERNAL_REFERENCES;
            case "sourceFulfillmentProductId" -> ProductField.SOURCE_FULFILLMENT_PRODUCT_ID;
            case "importantFeatures" -> ProductField.IMPORTANT_FEATURES;
            case "externalAttributes" -> ProductField.EXTERNAL_ATTRIBUTES;
            case "externalCategories" -> ProductField.EXTERNAL_CATEGORIES;
            case "externalVariantGroup" -> ProductField.EXTERNAL_VARIANT_GROUP;
            case "externalResponsibleProducer" -> ProductField.EXTERNAL_RESPONSIBLE_PRODUCER;
            case "externalResponsiblePerson" -> ProductField.EXTERNAL_RESPONSIBLE_PERSON;
            case "images" -> ProductField.IMAGES;
            case "files" -> ProductField.FILES;
            case "price" -> ProductField.PRICE;
            case "mobilePrice" -> ProductField.MOBILE_PRICE;
            case "cataloguePrice" -> ProductField.CATALOGUE_PRICE;
            case "referencePriceType" -> ProductField.REFERENCE_PRICE_TYPE;
            case "stock" -> ProductField.STOCK;
            case "status" -> ProductField.STATUS;
            case "archived" -> ProductField.ARCHIVED;
            case "dispatchTime" -> ProductField.DISPATCH_TIME;
            case "deliveryPriceList" -> ProductField.DELIVERY_PRICE_LIST;
            case "weight" -> ProductField.WEIGHT;
            case "obligatoryIdentifier" -> ProductField.OBLIGATORY_IDENTIFIER;
            case "voluntaryIdentifier" -> ProductField.VOLUNTARY_IDENTIFIER;
            case "returnIdentifier" -> ProductField.RETURN_IDENTIFIER;
            case "invoiceType" -> ProductField.INVOICE_TYPE;
            case "taxRate" -> ProductField.TAX_RATE;
            case "basketLimit" -> ProductField.BASKET_LIMIT;
            case "energyLabel" -> ProductField.ENERGY_LABEL;
            case "instructionWithSafetyInformation" -> ProductField.INSTRUCTION_WITH_SAFETY_INFORMATION;
            case "informationCard" -> ProductField.INFORMATION_CARD;
            case "producerId" -> ProductField.PRODUCER_ID;
            case "responsiblePersonId" -> ProductField.RESPONSIBLE_PERSON_ID;
            case "externalMetaProductId" -> ProductField.EXTERNAL_META_PRODUCT_ID;
            case "externalProductSets" -> ProductField.EXTERNAL_PRODUCT_SETS;
            case "productSets" -> ProductField.PRODUCT_SETS;
            case "productAttachments" -> ProductField.PRODUCT_ATTACHMENTS;
            case "automaticDiscountRuleId" -> ProductField.AUTOMATIC_DISCOUNT_RULE_ID;
            case "markets" -> ProductField.MARKETS;
            case "translations" -> ProductField.TRANSLATIONS;
            case "externalId" -> ProductField.EXTERNAL_ID;
            case "externalDescriptionHash" -> ProductField.EXTERNAL_DESCRIPTION_HASH;
            case "externalDescription" -> ProductField.EXTERNAL_DESCRIPTION;
            case "attributes" -> ProductField.ATTRIBUTES;
            case "categories" -> ProductField.CATEGORIES;
            case "packaging" -> ProductField.PACKAGING;
            case "marketplaceId" -> ProductField.MARKETPLACE_ID;
            case "slug" -> ProductField.SLUG;
            case "buyableProblems" -> ProductField.BUYABLE_PROBLEMS;
            case "archivedAt" -> ProductField.ARCHIVED_AT;
            case "frozen" -> ProductField.FROZEN;
            case "created" -> ProductField.CREATED;
            case "updated" -> ProductField.UPDATED;
            case "producerIds" -> ProductField.PRODUCER_IDS;
            case "responsiblePersonIds" -> ProductField.RESPONSIBLE_PERSON_IDS;
            default -> null;
        };
    }
}
