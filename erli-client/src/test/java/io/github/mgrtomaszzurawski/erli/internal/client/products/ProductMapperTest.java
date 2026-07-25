package io.github.mgrtomaszzurawski.erli.internal.client.products;

import io.github.mgrtomaszzurawski.erli.core.model.TaxRate;
import io.github.mgrtomaszzurawski.erli.domain.products.AttachmentKind;
import io.github.mgrtomaszzurawski.erli.domain.products.AttributeValues;
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
import io.github.mgrtomaszzurawski.erli.domain.products.Product;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductStatus;
import io.github.mgrtomaszzurawski.erli.domain.products.ReferencePriceType;
import io.github.mgrtomaszzurawski.erli.domain.products.ResponsibleEntitySource;
import io.github.mgrtomaszzurawski.erli.domain.products.VariantGroupSource;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Field-level assertions on the products read mapper.
 *
 * <p>Fixture provenance: {@code fixtures/products/product-full.json} is derived from the vendored
 * {@code openapi/swagger.json} — the {@code ProductCreate} example plus every {@code ProductResponse}
 * property the schema defines — because the sandbox shop is empty and no real product payload could be
 * observed yet. Bucket A's live write→read seeds the shop; the fixture is to be reconciled against the
 * observed payload then (see BACKLOG / KNOWN-SERVER-BEHAVIORS).
 *
 * <p>These assert values, not "no exception was thrown": a mapper that silently drops a nested field
 * would pass the latter and fail here.
 */
class ProductMapperTest {

    private static final String FULL_PRODUCT_FIXTURE = "/fixtures/products/product-full.json";

    private static Product product;

    @BeforeAll
    static void mapFixture() throws IOException {
        product = ProductMapper.toDomain(decodeFixture(FULL_PRODUCT_FIXTURE));
    }

    private static ProductResponse decodeFixture(String resource) throws IOException {
        try (InputStream stream = ProductMapperTest.class.getResourceAsStream(resource)) {
            return new JsonCodec().read(new String(stream.readAllBytes(), StandardCharsets.UTF_8),
                    ProductResponse.class);
        }
    }

    @Test
    void mapsIdentityAndLifecycleFields() {
        assertEquals("sku-winter-jacket-1", product.externalId().value());
        assertEquals(987654L, product.marketplaceId());
        assertEquals("Kurtka zimowa z kapturem", product.name());
        assertEquals("kurtka-zimowa-z-kapturem", product.slug());
        assertEquals(ProductStatus.ACTIVE, product.status());
        assertEquals(10, product.stock());
        assertFalse(product.archived());
        assertTrue(product.archivedAt().isEmpty());
        assertEquals(2026, product.created().getYear());
        assertTrue(product.updated().isPresent());
        assertTrue(product.buyableProblems().isEmpty());
        assertTrue(product.isBuyable());
    }

    @Test
    void convertsGroszePricesToPln() {
        assertEquals(new BigDecimal("100.00"), product.price().amount());
        assertEquals("PLN", product.price().currency().getCurrencyCode());
        assertEquals(new BigDecimal("95.00"), product.mobilePrice().orElseThrow().amount());
        assertEquals(new BigDecimal("200.00"), product.cataloguePrice().orElseThrow().amount());
        assertEquals(ReferencePriceType.CATALOGUE_PRICE, product.referencePriceType().orElseThrow());
    }

    @Test
    void mapsStructuredDescriptionBlocks() {
        var sections = product.description().orElseThrow().sections();
        assertEquals(1, sections.size());
        var items = sections.get(0).items();
        assertEquals(2, items.size());
        assertEquals(DescriptionItemType.TEXT, items.get(0).type());
        assertEquals("<h1>Kurtka zimowa z kapturem</h1>", items.get(0).content().orElseThrow());
        assertEquals(DescriptionItemType.IMAGE, items.get(1).type());
        assertEquals("https://example.com/section.jpg", items.get(1).url().orElseThrow());
        assertEquals("Ciepła kurtka na zimę", product.externalDescription().orElseThrow());
        assertEquals("b1946ac92492d2347c6235b4d2611184", product.externalDescriptionHash().orElseThrow());
    }

    @Test
    void mapsImagesIncludingMarketplaceManagedFields() {
        var image = product.images().get(0);
        assertEquals("https://example.com/image.jpg", image.url());
        assertTrue(image.isVariantImage().orElseThrow());
        assertFalse(image.isLifestyleImage().orElseThrow());
        assertTrue(image.isFrozenImage().orElseThrow());
        assertEquals("https://shop.example.com/orig.jpg", image.originalExternalUrl().orElseThrow());
        assertEquals(ImageTransformation.CLEAN_WATERMARK, image.appliedTransformation().orElseThrow());
        assertEquals("https://cdn.erli.pl/image.jpg", image.internalUrl().orElseThrow());
        assertEquals("https://example.com/spec.pdf", product.files().get(0).url());
    }

    @Test
    void selectsTheMatchingExternalAttributeValueVariant() {
        var text = product.externalAttributes().get(0);
        assertEquals("Kolor", text.id().orElseThrow());
        assertEquals(ExternalSource.ALLEGRO, text.source().orElseThrow());
        assertEquals(ExternalAttributeType.STRING, text.type().orElseThrow());
        assertEquals(0, text.index().orElseThrow());
        assertEquals(java.util.List.of("Czarny"),
                assertInstanceOf(AttributeValues.TextValues.class, text.values()).texts());

        var range = product.externalAttributes().get(1);
        assertEquals(ExternalAttributeType.RANGE, range.type().orElseThrow());
        assertEquals("cm", range.unit().orElseThrow());
        var bounds = assertInstanceOf(AttributeValues.RangeValues.class, range.values());
        assertEquals(new BigDecimal("38"), bounds.from().orElseThrow());
        assertEquals(new BigDecimal("44"), bounds.to().orElseThrow());
    }

    @Test
    void mapsResolvedAttributesAndCategoryPaths() {
        var attribute = product.attributes().get(0);
        assertEquals(new BigDecimal("501"), attribute.id().orElseThrow());
        assertEquals("Kolor", attribute.name().orElseThrow());
        assertEquals("szt", attribute.unit().orElseThrow());
        assertEquals(java.util.List.of(new BigDecimal("9001")), attribute.valueIds());
        var entries = assertInstanceOf(AttributeValues.DictionaryValues.class, attribute.values()).entries();
        assertEquals("9001", entries.get(0).id());
        assertEquals("Czarny", entries.get(0).name().orElseThrow());

        assertEquals(1, product.categories().size());
        var path = product.categories().get(0);
        assertEquals(2, path.size());
        assertEquals("Odzież", path.get(0).name().orElseThrow());
        assertEquals(new BigDecimal("11"), path.get(1).id().orElseThrow());
    }

    @Test
    void mapsExternalCategoriesReferencesAndVariantGroup() {
        var category = product.externalCategories().get(0);
        assertEquals(ExternalSource.ALLEGRO, category.source().orElseThrow());
        assertEquals(0, category.index().orElseThrow());
        assertEquals("Odzież damska", category.breadcrumb().get(1).name().orElseThrow());
        assertEquals("2", category.breadcrumb().get(1).id());

        var reference = product.externalReferences().get(0);
        assertEquals("8885579723", reference.id().orElseThrow());
        assertEquals(ExternalReferenceKind.ALLEGRO, reference.kind().orElseThrow());
        assertEquals(ExternalReferenceSource.API, reference.source().orElseThrow());
        assertEquals("https://allegro.pl/oferta/8885579723", reference.url().orElseThrow());

        var group = product.externalVariantGroup().orElseThrow();
        assertEquals("variant-group-7", group.id().orElseThrow());
        assertEquals(VariantGroupSource.INTEGRATION, group.source().orElseThrow());
        assertEquals(java.util.List.of("Kolor", "Rozmiar"), group.attributes());
    }

    @Test
    void mapsResponsibleEntitiesSetsAndAttachments() {
        assertEquals("producer-1", product.externalResponsibleProducer().get(0).externalId().orElseThrow());
        assertEquals(ResponsibleEntitySource.API,
                product.externalResponsibleProducer().get(0).source().orElseThrow());
        assertEquals(ResponsibleEntitySource.MANUAL,
                product.externalResponsiblePerson().get(0).source().orElseThrow());

        assertEquals("meta-2",
                product.externalProductSets().orElseThrow().items().get(0).externalMetaProductId().orElseThrow());
        assertEquals(new BigDecimal("2"),
                product.externalProductSets().orElseThrow().items().get(0).quantity().orElseThrow());
        assertEquals(4242, product.productSets().orElseThrow().items().get(0).metaProductId().orElseThrow());

        var attachment = product.productAttachments().get(0);
        assertEquals(77, attachment.id().orElseThrow());
        assertEquals(AttachmentKind.ENERGY_LABEL, attachment.kind().orElseThrow());
        assertEquals(java.util.List.of(Market.PL, Market.DE), attachment.markets());
    }

    @Test
    void mapsTranslationsPerMarket() {
        var translations = product.translations().orElseThrow();
        var polish = translations.polish().orElseThrow();
        assertEquals("Kurtka zimowa z kapturem", polish.name().orElseThrow());
        assertEquals(new BigDecimal("3001"), polish.descriptionId().orElseThrow());
        assertEquals("kolor", polish.attributes().get(0).key().orElseThrow());
        assertEquals("szt", polish.attributes().get(0).unit().orElseThrow());
        assertEquals("Winterjacke mit Kapuze", translations.german().orElseThrow().name().orElseThrow());
    }

    @Test
    void mapsLogisticsTaxAndIdentifierFields() {
        assertEquals(DispatchTimeUnit.DAY, product.dispatchTime().unit());
        assertEquals(1, product.dispatchTime().period());
        assertEquals("standard", product.deliveryPriceList().orElseThrow());
        assertEquals(new BigDecimal("1.25"), product.weight().orElseThrow());
        assertEquals(java.util.List.of("karton"), product.packaging().orElseThrow().tags());
        assertEquals(new BigDecimal("1.5"), product.packaging().orElseThrow().weight().orElseThrow());
        assertEquals(5, product.basketLimit().orElseThrow());
        assertEquals(InvoiceType.VAT_INVOICE, product.invoiceType().orElseThrow());
        assertEquals(TaxRate.TAX_23, product.taxRate().orElseThrow());
        assertEquals("OBL-1", product.obligatoryIdentifier().orElseThrow());
        assertEquals("VOL-1", product.voluntaryIdentifier().orElseThrow());
        assertEquals("RET-1", product.returnIdentifier().orElseThrow());
        assertEquals("https://example.com/energy.pdf", product.energyLabel().orElseThrow());
        assertEquals("https://example.com/safety.pdf",
                product.instructionWithSafetyInformation().orElseThrow());
        assertEquals("https://example.com/card.pdf", product.informationCard().orElseThrow());
    }

    @Test
    void mapsCatalogMarketAndFulfilmentReferences() {
        assertEquals(BaseMarket.PL, product.baseMarket().orElseThrow());
        assertEquals(Market.PL, product.markets().orElseThrow());
        assertEquals("5901234123457", product.ean().orElseThrow());
        assertEquals("SKU123", product.sku().orElseThrow());
        assertEquals(java.util.List.of("wodoodporna", "ocieplana"), product.importantFeatures());
        assertEquals("meta-1", product.externalMetaProductId().orElseThrow());
        assertEquals(new BigDecimal("601"), product.producerId().orElseThrow());
        assertEquals(java.util.List.of(new BigDecimal("601"), new BigDecimal("602")), product.producerIds());
        assertEquals(new BigDecimal("701"), product.responsiblePersonId().orElseThrow());
        assertEquals(java.util.List.of(new BigDecimal("701")), product.responsiblePersonIds());
        assertEquals(808, product.sourceFulfillmentProductId().orElseThrow());
        assertEquals(909, product.automaticDiscountRuleId().orElseThrow());
    }

    @Test
    void collectsOnlyTheFlagsThatAreSetIntoFrozenFields() {
        assertTrue(product.frozen().isFrozen(ProductField.PRICE));
        assertTrue(product.frozen().isFrozen(ProductField.STOCK));
        assertFalse(product.frozen().isFrozen(ProductField.NAME));
        assertFalse(product.frozen().isFrozen(ProductField.EAN));
        assertEquals(java.util.Set.of(ProductField.PRICE, ProductField.STOCK), product.frozen().fields());
    }

    @Test
    void appliesTheDayDefaultWhenDispatchUnitIsAbsent() throws IOException {
        ProductResponse rawProduct = decodeFixture(FULL_PRODUCT_FIXTURE);
        rawProduct.getDispatchTime().setUnit(null);
        assertEquals(DispatchTimeUnit.DAY, ProductMapper.toDomain(rawProduct).dispatchTime().unit());
    }

    @Test
    void rejectsAResponseMissingARequiredField() throws IOException {
        ProductResponse rawProduct = decodeFixture(FULL_PRODUCT_FIXTURE);
        rawProduct.setPrice(null);
        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> ProductMapper.toDomain(rawProduct));
        assertTrue(failure.getMessage().contains("price"), failure.getMessage());
    }
}
