package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.domain.products.AttachmentKind;
import io.github.mgrtomaszzurawski.erli.domain.products.BaseMarket;
import io.github.mgrtomaszzurawski.erli.domain.products.DescriptionItemType;
import io.github.mgrtomaszzurawski.erli.domain.products.DispatchTimeUnit;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalAttributeType;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalReferenceKind;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalReferenceSource;
import io.github.mgrtomaszzurawski.erli.domain.products.ExternalSource;
import io.github.mgrtomaszzurawski.erli.domain.products.ImageTransformation;
import io.github.mgrtomaszzurawski.erli.domain.products.InvoiceType;
import io.github.mgrtomaszzurawski.erli.domain.products.Market;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductSortField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductStatus;
import io.github.mgrtomaszzurawski.erli.domain.products.ReferencePriceType;
import io.github.mgrtomaszzurawski.erli.domain.products.ResponsibleEntitySource;
import io.github.mgrtomaszzurawski.erli.domain.products.SortOrder;
import io.github.mgrtomaszzurawski.erli.domain.products.TaxRate;
import io.github.mgrtomaszzurawski.erli.domain.products.VariantGroupSource;

/**
 * Translates between the products domain enums and the wire values the Erli API uses.
 *
 * <p>The generator emits a separate Java enum per <em>occurrence</em> of a spec enum, so one spec enum
 * such as the attribute {@code source} exists at Layer 1 as four structurally identical types (one per
 * {@code anyOf} branch). Switching over each of those would mean twenty near-duplicate mappings that all
 * say the same thing. This class therefore pivots on the wire value, which is the thing the spec actually
 * defines, and every Layer-1 enum exposes it as {@code getValue()} / accepts it via {@code fromValue(...)}.
 *
 * <p>The compile-time safety the core convention asks for is kept where it belongs — on the SDK's own
 * enums: every {@code wireNameOf} is an exhaustive switch, so adding a domain constant without giving it
 * a wire value is a compile error. In the reading direction an unrecognised value throws rather than
 * silently degrading, naming both the value and the enum so a spec addition is obvious in the message.
 * Internal: never exported.
 */
final class ProductEnums {

    private ProductEnums() {
    }

    /** The {@link ProductStatus} a wire value denotes. */
    static ProductStatus toProductStatus(String wireValue) {
        return switch (wireValue) {
            case "active" -> ProductStatus.ACTIVE;
            case "inactive" -> ProductStatus.INACTIVE;
            default -> throw unknown("ProductStatus", wireValue);
        };
    }

    /** The wire value of a {@link ProductStatus}. */
    static String wireNameOf(ProductStatus value) {
        return switch (value) {
            case ACTIVE -> "active";
            case INACTIVE -> "inactive";
        };
    }

    /** The {@link BaseMarket} a wire value denotes. */
    static BaseMarket toBaseMarket(String wireValue) {
        return switch (wireValue) {
            case "pl" -> BaseMarket.PL;
            case "de" -> BaseMarket.DE;
            default -> throw unknown("BaseMarket", wireValue);
        };
    }

    /** The wire value of a {@link BaseMarket}. */
    static String wireNameOf(BaseMarket value) {
        return switch (value) {
            case PL -> "pl";
            case DE -> "de";
        };
    }

    /** The {@link Market} a wire value denotes. */
    static Market toMarket(String wireValue) {
        return switch (wireValue) {
            case "pl" -> Market.PL;
            case "de" -> Market.DE;
            default -> throw unknown("Market", wireValue);
        };
    }

    /** The wire value of a {@link Market}. */
    static String wireNameOf(Market value) {
        return switch (value) {
            case PL -> "pl";
            case DE -> "de";
        };
    }

    /** The {@link InvoiceType} a wire value denotes. */
    static InvoiceType toInvoiceType(String wireValue) {
        return switch (wireValue) {
            case "vatInvoice" -> InvoiceType.VAT_INVOICE;
            case "vatInvoiceWithMarginScheme" -> InvoiceType.VAT_INVOICE_WITH_MARGIN_SCHEME;
            case "invoiceWithoutVat" -> InvoiceType.INVOICE_WITHOUT_VAT;
            case "withoutInvoice" -> InvoiceType.WITHOUT_INVOICE;
            default -> throw unknown("InvoiceType", wireValue);
        };
    }

    /** The wire value of a {@link InvoiceType}. */
    static String wireNameOf(InvoiceType value) {
        return switch (value) {
            case VAT_INVOICE -> "vatInvoice";
            case VAT_INVOICE_WITH_MARGIN_SCHEME -> "vatInvoiceWithMarginScheme";
            case INVOICE_WITHOUT_VAT -> "invoiceWithoutVat";
            case WITHOUT_INVOICE -> "withoutInvoice";
        };
    }

    /** The {@link TaxRate} a wire value denotes. */
    static TaxRate toTaxRate(String wireValue) {
        return switch (wireValue) {
            case "TAX_0" -> TaxRate.TAX_0;
            case "TAX_5" -> TaxRate.TAX_5;
            case "TAX_7" -> TaxRate.TAX_7;
            case "TAX_8" -> TaxRate.TAX_8;
            case "TAX_19" -> TaxRate.TAX_19;
            case "TAX_23" -> TaxRate.TAX_23;
            case "TAX_NP" -> TaxRate.TAX_NP;
            case "TAX_ZW" -> TaxRate.TAX_ZW;
            default -> throw unknown("TaxRate", wireValue);
        };
    }

    /** The wire value of a {@link TaxRate}. */
    static String wireNameOf(TaxRate value) {
        return switch (value) {
            case TAX_0 -> "TAX_0";
            case TAX_5 -> "TAX_5";
            case TAX_7 -> "TAX_7";
            case TAX_8 -> "TAX_8";
            case TAX_19 -> "TAX_19";
            case TAX_23 -> "TAX_23";
            case TAX_NP -> "TAX_NP";
            case TAX_ZW -> "TAX_ZW";
        };
    }

    /** The {@link ReferencePriceType} a wire value denotes. */
    static ReferencePriceType toReferencePriceType(String wireValue) {
        return switch (wireValue) {
            case "marketplaceReferencePrice" -> ReferencePriceType.MARKETPLACE_REFERENCE_PRICE;
            case "cataloguePrice" -> ReferencePriceType.CATALOGUE_PRICE;
            default -> throw unknown("ReferencePriceType", wireValue);
        };
    }

    /** The wire value of a {@link ReferencePriceType}. */
    static String wireNameOf(ReferencePriceType value) {
        return switch (value) {
            case MARKETPLACE_REFERENCE_PRICE -> "marketplaceReferencePrice";
            case CATALOGUE_PRICE -> "cataloguePrice";
        };
    }

    /** The {@link DispatchTimeUnit} a wire value denotes. */
    static DispatchTimeUnit toDispatchTimeUnit(String wireValue) {
        return switch (wireValue) {
            case "hour" -> DispatchTimeUnit.HOUR;
            case "day" -> DispatchTimeUnit.DAY;
            case "month" -> DispatchTimeUnit.MONTH;
            default -> throw unknown("DispatchTimeUnit", wireValue);
        };
    }

    /** The wire value of a {@link DispatchTimeUnit}. */
    static String wireNameOf(DispatchTimeUnit value) {
        return switch (value) {
            case HOUR -> "hour";
            case DAY -> "day";
            case MONTH -> "month";
        };
    }

    /** The {@link DescriptionItemType} a wire value denotes. */
    static DescriptionItemType toDescriptionItemType(String wireValue) {
        return switch (wireValue) {
            case "TEXT" -> DescriptionItemType.TEXT;
            case "IMAGE" -> DescriptionItemType.IMAGE;
            default -> throw unknown("DescriptionItemType", wireValue);
        };
    }

    /** The wire value of a {@link DescriptionItemType}. */
    static String wireNameOf(DescriptionItemType value) {
        return switch (value) {
            case TEXT -> "TEXT";
            case IMAGE -> "IMAGE";
        };
    }

    /** The {@link ExternalSource} a wire value denotes. */
    static ExternalSource toExternalSource(String wireValue) {
        return switch (wireValue) {
            case "shop" -> ExternalSource.SHOP;
            case "allegro" -> ExternalSource.ALLEGRO;
            case "marketplace" -> ExternalSource.MARKETPLACE;
            default -> throw unknown("ExternalSource", wireValue);
        };
    }

    /** The wire value of a {@link ExternalSource}. */
    static String wireNameOf(ExternalSource value) {
        return switch (value) {
            case SHOP -> "shop";
            case ALLEGRO -> "allegro";
            case MARKETPLACE -> "marketplace";
        };
    }

    /** The {@link ExternalAttributeType} a wire value denotes. */
    static ExternalAttributeType toExternalAttributeType(String wireValue) {
        return switch (wireValue) {
            case "number" -> ExternalAttributeType.NUMBER;
            case "range" -> ExternalAttributeType.RANGE;
            case "dictionary" -> ExternalAttributeType.DICTIONARY;
            case "string" -> ExternalAttributeType.STRING;
            default -> throw unknown("ExternalAttributeType", wireValue);
        };
    }

    /** The wire value of a {@link ExternalAttributeType}. */
    static String wireNameOf(ExternalAttributeType value) {
        return switch (value) {
            case NUMBER -> "number";
            case RANGE -> "range";
            case DICTIONARY -> "dictionary";
            case STRING -> "string";
        };
    }

    /** The {@link VariantGroupSource} a wire value denotes. */
    static VariantGroupSource toVariantGroupSource(String wireValue) {
        return switch (wireValue) {
            case "marketplace" -> VariantGroupSource.MARKETPLACE;
            case "integration" -> VariantGroupSource.INTEGRATION;
            default -> throw unknown("VariantGroupSource", wireValue);
        };
    }

    /** The wire value of a {@link VariantGroupSource}. */
    static String wireNameOf(VariantGroupSource value) {
        return switch (value) {
            case MARKETPLACE -> "marketplace";
            case INTEGRATION -> "integration";
        };
    }

    /** The {@link ResponsibleEntitySource} a wire value denotes. */
    static ResponsibleEntitySource toResponsibleEntitySource(String wireValue) {
        return switch (wireValue) {
            case "api" -> ResponsibleEntitySource.API;
            case "manual" -> ResponsibleEntitySource.MANUAL;
            case "allegro" -> ResponsibleEntitySource.ALLEGRO;
            case "idosell" -> ResponsibleEntitySource.IDOSELL;
            case "prestaShop" -> ResponsibleEntitySource.PRESTA_SHOP;
            case "shoper" -> ResponsibleEntitySource.SHOPER;
            case "baselinker" -> ResponsibleEntitySource.BASELINKER;
            default -> throw unknown("ResponsibleEntitySource", wireValue);
        };
    }

    /** The wire value of a {@link ResponsibleEntitySource}. */
    static String wireNameOf(ResponsibleEntitySource value) {
        return switch (value) {
            case API -> "api";
            case MANUAL -> "manual";
            case ALLEGRO -> "allegro";
            case IDOSELL -> "idosell";
            case PRESTA_SHOP -> "prestaShop";
            case SHOPER -> "shoper";
            case BASELINKER -> "baselinker";
        };
    }

    /** The {@link ExternalReferenceKind} a wire value denotes. */
    static ExternalReferenceKind toExternalReferenceKind(String wireValue) {
        return switch (wireValue) {
            case "allegro" -> ExternalReferenceKind.ALLEGRO;
            case "ceneo" -> ExternalReferenceKind.CENEO;
            case "amazon" -> ExternalReferenceKind.AMAZON;
            case "empik" -> ExternalReferenceKind.EMPIK;
            case "morele" -> ExternalReferenceKind.MORELE;
            case "arena" -> ExternalReferenceKind.ARENA;
            case "other" -> ExternalReferenceKind.OTHER;
            case "custom" -> ExternalReferenceKind.CUSTOM;
            default -> throw unknown("ExternalReferenceKind", wireValue);
        };
    }

    /** The wire value of a {@link ExternalReferenceKind}. */
    static String wireNameOf(ExternalReferenceKind value) {
        return switch (value) {
            case ALLEGRO -> "allegro";
            case CENEO -> "ceneo";
            case AMAZON -> "amazon";
            case EMPIK -> "empik";
            case MORELE -> "morele";
            case ARENA -> "arena";
            case OTHER -> "other";
            case CUSTOM -> "custom";
        };
    }

    /** The {@link ExternalReferenceSource} a wire value denotes. */
    static ExternalReferenceSource toExternalReferenceSource(String wireValue) {
        return switch (wireValue) {
            case "scrapper" -> ExternalReferenceSource.SCRAPPER;
            case "profitwatch" -> ExternalReferenceSource.PROFITWATCH;
            case "api" -> ExternalReferenceSource.API;
            case "manual" -> ExternalReferenceSource.MANUAL;
            case "local" -> ExternalReferenceSource.LOCAL;
            case "integration-rss" -> ExternalReferenceSource.INTEGRATION_RSS;
            default -> throw unknown("ExternalReferenceSource", wireValue);
        };
    }

    /** The wire value of a {@link ExternalReferenceSource}. */
    static String wireNameOf(ExternalReferenceSource value) {
        return switch (value) {
            case SCRAPPER -> "scrapper";
            case PROFITWATCH -> "profitwatch";
            case API -> "api";
            case MANUAL -> "manual";
            case LOCAL -> "local";
            case INTEGRATION_RSS -> "integration-rss";
        };
    }

    /** The {@link ImageTransformation} a wire value denotes. */
    static ImageTransformation toImageTransformation(String wireValue) {
        return switch (wireValue) {
            case "clean-watermark" -> ImageTransformation.CLEAN_WATERMARK;
            case "ai" -> ImageTransformation.AI;
            default -> throw unknown("ImageTransformation", wireValue);
        };
    }

    /** The wire value of a {@link ImageTransformation}. */
    static String wireNameOf(ImageTransformation value) {
        return switch (value) {
            case CLEAN_WATERMARK -> "clean-watermark";
            case AI -> "ai";
        };
    }

    /** The {@link AttachmentKind} a wire value denotes. */
    static AttachmentKind toAttachmentKind(String wireValue) {
        return switch (wireValue) {
            case "guide" -> AttachmentKind.GUIDE;
            case "promotionRules" -> AttachmentKind.PROMOTION_RULES;
            case "contestRules" -> AttachmentKind.CONTEST_RULES;
            case "bookSnippet" -> AttachmentKind.BOOK_SNIPPET;
            case "userManual" -> AttachmentKind.USER_MANUAL;
            case "assemblyInstructions" -> AttachmentKind.ASSEMBLY_INSTRUCTIONS;
            case "gameInstructions" -> AttachmentKind.GAME_INSTRUCTIONS;
            case "safetyGuide" -> AttachmentKind.SAFETY_GUIDE;
            case "energyLabel" -> AttachmentKind.ENERGY_LABEL;
            case "productCard" -> AttachmentKind.PRODUCT_CARD;
            case "tireLabel" -> AttachmentKind.TIRE_LABEL;
            case "dataProcessingSoftware" -> AttachmentKind.DATA_PROCESSING_SOFTWARE;
            case "dataProcessingHardware" -> AttachmentKind.DATA_PROCESSING_HARDWARE;
            case "safetyDataSheet" -> AttachmentKind.SAFETY_DATA_SHEET;
            case "plantProtectionLicense" -> AttachmentKind.PLANT_PROTECTION_LICENSE;
            case "recyclingInfo" -> AttachmentKind.RECYCLING_INFO;
            default -> throw unknown("AttachmentKind", wireValue);
        };
    }

    /** The wire value of a {@link AttachmentKind}. */
    static String wireNameOf(AttachmentKind value) {
        return switch (value) {
            case GUIDE -> "guide";
            case PROMOTION_RULES -> "promotionRules";
            case CONTEST_RULES -> "contestRules";
            case BOOK_SNIPPET -> "bookSnippet";
            case USER_MANUAL -> "userManual";
            case ASSEMBLY_INSTRUCTIONS -> "assemblyInstructions";
            case GAME_INSTRUCTIONS -> "gameInstructions";
            case SAFETY_GUIDE -> "safetyGuide";
            case ENERGY_LABEL -> "energyLabel";
            case PRODUCT_CARD -> "productCard";
            case TIRE_LABEL -> "tireLabel";
            case DATA_PROCESSING_SOFTWARE -> "dataProcessingSoftware";
            case DATA_PROCESSING_HARDWARE -> "dataProcessingHardware";
            case SAFETY_DATA_SHEET -> "safetyDataSheet";
            case PLANT_PROTECTION_LICENSE -> "plantProtectionLicense";
            case RECYCLING_INFO -> "recyclingInfo";
        };
    }

    /** The {@link ProductSortField} a wire value denotes. */
    static ProductSortField toProductSortField(String wireValue) {
        return switch (wireValue) {
            case "externalId" -> ProductSortField.EXTERNAL_ID;
            case "marketplaceId" -> ProductSortField.MARKETPLACE_ID;
            case "name" -> ProductSortField.NAME;
            case "ean" -> ProductSortField.EAN;
            case "sku" -> ProductSortField.SKU;
            case "created" -> ProductSortField.CREATED;
            case "updated" -> ProductSortField.UPDATED;
            case "archivedAt" -> ProductSortField.ARCHIVED_AT;
            default -> throw unknown("ProductSortField", wireValue);
        };
    }

    /** The wire value of a {@link ProductSortField}. */
    static String wireNameOf(ProductSortField value) {
        return switch (value) {
            case EXTERNAL_ID -> "externalId";
            case MARKETPLACE_ID -> "marketplaceId";
            case NAME -> "name";
            case EAN -> "ean";
            case SKU -> "sku";
            case CREATED -> "created";
            case UPDATED -> "updated";
            case ARCHIVED_AT -> "archivedAt";
        };
    }

    /** The {@link SortOrder} a wire value denotes. */
    static SortOrder toSortOrder(String wireValue) {
        return switch (wireValue) {
            case "ASC" -> SortOrder.ASC;
            case "DESC" -> SortOrder.DESC;
            default -> throw unknown("SortOrder", wireValue);
        };
    }

    /** The wire value of a {@link SortOrder}. */
    static String wireNameOf(SortOrder value) {
        return switch (value) {
            case ASC -> "ASC";
            case DESC -> "DESC";
        };
    }

    /** Reading a value the spec did not have when this SDK version was built. */
    private static IllegalStateException unknown(String enumName, String wireValue) {
        return new IllegalStateException(
                "Erli returned an unknown " + enumName + " value: '" + wireValue
                        + "'. The API has likely gained a new value; upgrade the SDK.");
    }
}
