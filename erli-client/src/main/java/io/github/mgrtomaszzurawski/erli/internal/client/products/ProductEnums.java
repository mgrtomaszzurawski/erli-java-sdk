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
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductStatus;
import io.github.mgrtomaszzurawski.erli.domain.products.ReferencePriceType;
import io.github.mgrtomaszzurawski.erli.domain.products.ResponsibleEntitySource;
import io.github.mgrtomaszzurawski.erli.domain.products.TaxRate;
import io.github.mgrtomaszzurawski.erli.domain.products.VariantGroupSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Translates between the products domain enums and the wire values the Erli API uses.
 *
 * <p>Each domain enum carries its own wire value ({@code SomeEnum.wireName()}), so the writing direction
 * needs no table at all and no wire literal appears in this class. The reading direction is one lookup
 * map per enum, built once from {@code values()} — which also means a constant can never be reachable in
 * one direction only, the failure mode a hand-written pair of switches invites.
 *
 * <p>The generator emits a separate Java enum per <em>occurrence</em> of a spec enum, so one spec enum
 * such as the attribute {@code source} exists at Layer 1 as four structurally identical types. Pivoting
 * on the wire value — the thing the spec actually defines, and what every Layer-1 enum exposes as
 * {@code getValue()} — collapses those four back into one mapping.
 *
 * <p><strong>Where an unrecognised value actually surfaces (CORE-12).</strong> The shared codec decodes
 * with {@code READ_UNKNOWN_ENUM_VALUES_AS_NULL}, so a value the SDK does not know never reaches this
 * class when the API declares the field as an enum — Layer 1 hands the mapper a {@code null} first, and
 * the mapper decides: an optional field becomes absent, a required one fails its {@code require(...)}
 * check. The lookups here only see a raw value for the handful of properties the spec types as an
 * array of enums, which Layer 1 exposes as {@code List<String>}.
 *
 * <p>Those are the ones that need a policy, because they arrive as a list: rejecting one unfamiliar
 * entry would fail the whole product read. {@link #toMarketOrNull} and
 * {@link #toProductFieldOrNull} therefore answer {@code null} rather than throwing, and their callers
 * keep the raw value so a round trip loses nothing. Everything else stays fail-loud — reaching it with an unknown value would mean
 * the codec's tolerance had been turned off, which is worth hearing about.
 *
 * <p>Internal: never exported.
 */
final class ProductEnums {

    private static final Map<String, ProductStatus> PRODUCT_STATUSES = index(ProductStatus.values(), ProductStatus::wireName);
    private static final Map<String, BaseMarket> BASE_MARKETS = index(BaseMarket.values(), BaseMarket::wireName);
    private static final Map<String, Market> MARKETS = index(Market.values(), Market::wireName);
    private static final Map<String, InvoiceType> INVOICE_TYPES = index(InvoiceType.values(), InvoiceType::wireName);
    private static final Map<String, TaxRate> TAX_RATES = index(TaxRate.values(), TaxRate::wireName);
    private static final Map<String, ReferencePriceType> REFERENCE_PRICE_TYPES =
            index(ReferencePriceType.values(), ReferencePriceType::wireName);
    private static final Map<String, DispatchTimeUnit> DISPATCH_TIME_UNITS =
            index(DispatchTimeUnit.values(), DispatchTimeUnit::wireName);
    private static final Map<String, DescriptionItemType> DESCRIPTION_ITEM_TYPES =
            index(DescriptionItemType.values(), DescriptionItemType::wireName);
    private static final Map<String, ExternalSource> EXTERNAL_SOURCES =
            index(ExternalSource.values(), ExternalSource::wireName);
    private static final Map<String, ExternalAttributeType> EXTERNAL_ATTRIBUTE_TYPES =
            index(ExternalAttributeType.values(), ExternalAttributeType::wireName);
    private static final Map<String, VariantGroupSource> VARIANT_GROUP_SOURCES =
            index(VariantGroupSource.values(), VariantGroupSource::wireName);
    private static final Map<String, ResponsibleEntitySource> RESPONSIBLE_ENTITY_SOURCES =
            index(ResponsibleEntitySource.values(), ResponsibleEntitySource::wireName);
    private static final Map<String, ExternalReferenceKind> EXTERNAL_REFERENCE_KINDS =
            index(ExternalReferenceKind.values(), ExternalReferenceKind::wireName);
    private static final Map<String, ExternalReferenceSource> EXTERNAL_REFERENCE_SOURCES =
            index(ExternalReferenceSource.values(), ExternalReferenceSource::wireName);
    private static final Map<String, ImageTransformation> IMAGE_TRANSFORMATIONS =
            index(ImageTransformation.values(), ImageTransformation::wireName);
    private static final Map<String, AttachmentKind> ATTACHMENT_KINDS =
            index(AttachmentKind.values(), AttachmentKind::wireName);
    private static final Map<String, ProductField> PRODUCT_FIELDS = index(ProductField.values(), ProductField::wireName);

    private ProductEnums() {
    }

    static ProductStatus toProductStatus(String wireValue) {
        return lookup(PRODUCT_STATUSES, wireValue, ProductStatus.class);
    }

    static BaseMarket toBaseMarket(String wireValue) {
        return lookup(BASE_MARKETS, wireValue, BaseMarket.class);
    }

    /**
     * The market a wire value denotes, or {@code null} when this SDK version does not know it — the
     * caller keeps the raw value rather than losing it. Reached with a raw value only for
     * {@code productAttachments[].markets}, which the spec types as an array of enums and Layer 1
     * exposes as {@code List<String>}.
     */
    static Market toMarketOrNull(String wireValue) {
        return MARKETS.get(wireValue);
    }

    static InvoiceType toInvoiceType(String wireValue) {
        return lookup(INVOICE_TYPES, wireValue, InvoiceType.class);
    }

    static TaxRate toTaxRate(String wireValue) {
        return lookup(TAX_RATES, wireValue, TaxRate.class);
    }

    static ReferencePriceType toReferencePriceType(String wireValue) {
        return lookup(REFERENCE_PRICE_TYPES, wireValue, ReferencePriceType.class);
    }

    static DispatchTimeUnit toDispatchTimeUnit(String wireValue) {
        return lookup(DISPATCH_TIME_UNITS, wireValue, DispatchTimeUnit.class);
    }

    static DescriptionItemType toDescriptionItemType(String wireValue) {
        return lookup(DESCRIPTION_ITEM_TYPES, wireValue, DescriptionItemType.class);
    }

    static ExternalSource toExternalSource(String wireValue) {
        return lookup(EXTERNAL_SOURCES, wireValue, ExternalSource.class);
    }

    static ExternalAttributeType toExternalAttributeType(String wireValue) {
        return lookup(EXTERNAL_ATTRIBUTE_TYPES, wireValue, ExternalAttributeType.class);
    }

    static VariantGroupSource toVariantGroupSource(String wireValue) {
        return lookup(VARIANT_GROUP_SOURCES, wireValue, VariantGroupSource.class);
    }

    static ResponsibleEntitySource toResponsibleEntitySource(String wireValue) {
        return lookup(RESPONSIBLE_ENTITY_SOURCES, wireValue, ResponsibleEntitySource.class);
    }

    static ExternalReferenceKind toExternalReferenceKind(String wireValue) {
        return lookup(EXTERNAL_REFERENCE_KINDS, wireValue, ExternalReferenceKind.class);
    }

    static ExternalReferenceSource toExternalReferenceSource(String wireValue) {
        return lookup(EXTERNAL_REFERENCE_SOURCES, wireValue, ExternalReferenceSource.class);
    }

    static ImageTransformation toImageTransformation(String wireValue) {
        return lookup(IMAGE_TRANSFORMATIONS, wireValue, ImageTransformation.class);
    }

    static AttachmentKind toAttachmentKind(String wireValue) {
        return lookup(ATTACHMENT_KINDS, wireValue, AttachmentKind.class);
    }

    /**
     * The field a wire name denotes, or {@code null} when the marketplace names a field this SDK version
     * does not know. Unknown names are skipped rather than fatal here: the API may add fields at any
     * time, and reading {@code updatedFields} or {@code frozen} must not fail because a newer one
     * appeared.
     */
    static ProductField toProductFieldOrNull(String wireValue) {
        return PRODUCT_FIELDS.get(wireValue);
    }

    private static <T extends Enum<T>> Map<String, T> index(T[] constants, Function<T, String> wireName) {
        Map<String, T> byWireName = new HashMap<>();
        for (T constant : constants) {
            T clashing = byWireName.put(wireName.apply(constant), constant);
            if (clashing != null) {
                // Two constants claiming one wire value would leave one of them unreachable when reading.
                // Failing at class-init makes that a startup error rather than a silent mis-mapping.
                throw new IllegalStateException(constant.getDeclaringClass().getSimpleName()
                        + " has two constants with the wire value '" + wireName.apply(constant)
                        + "': " + clashing + " and " + constant);
            }
        }
        return Map.copyOf(byWireName);
    }

    private static <T> T lookup(Map<String, T> byWireName, String wireValue, Class<T> enumType) {
        T found = byWireName.get(wireValue);
        if (found == null) {
            throw new IllegalStateException(
                    "Erli returned an unknown " + enumType.getSimpleName() + " value: '" + wireValue
                            + "'. The API has likely gained a new value; upgrade the SDK.");
        }
        return found;
    }
}
