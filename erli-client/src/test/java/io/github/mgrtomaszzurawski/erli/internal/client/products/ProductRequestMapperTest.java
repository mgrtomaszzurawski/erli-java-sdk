package io.github.mgrtomaszzurawski.erli.internal.client.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;
import io.github.mgrtomaszzurawski.erli.core.model.TaxRate;
import io.github.mgrtomaszzurawski.erli.domain.products.BaseMarket;
import io.github.mgrtomaszzurawski.erli.domain.products.DispatchTime;
import io.github.mgrtomaszzurawski.erli.domain.products.InvoiceType;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductContent;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductDraft;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductField;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductImage;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductPatch;
import io.github.mgrtomaszzurawski.erli.domain.products.ProductStatus;
import io.github.mgrtomaszzurawski.erli.domain.products.ReferencePriceType;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductCreate;
import io.github.mgrtomaszzurawski.erli.rest.model.ProductUpdate;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Direct unit tests for {@link ProductRequestMapper}. The mapper is exercised end-to-end by
 * {@code ProductAccessTest}, but only for the handful of fields those flows happen to set — so most of
 * its ~40 scalar mappings were never asserted (a mutation-testing gap: the setters could be swapped or
 * dropped and every test stayed green). These tests populate the scalar, enum and price fields directly
 * and pin each mapped value, plus the {@code clear} path that sends an explicit {@code null}.
 */
class ProductRequestMapperTest {

    private static ProductContent.Builder everyScalarField() {
        return ProductContent.builder()
                .name("Kurtka zimowa")
                .ean("5901234123457")
                .sku("SKU-9001")
                .baseMarket(BaseMarket.PL)
                .sourceFulfillmentProductId(4242)
                .stock(17)
                .price(Money.ofPln("120.50"))
                .mobilePrice(Money.ofPln("118.00"))
                .cataloguePrice(Money.ofPln("150.00"))
                .referencePriceType(ReferencePriceType.CATALOGUE_PRICE)
                .status(ProductStatus.ACTIVE)
                .archived(Boolean.FALSE)
                .dispatchTime(DispatchTime.ofDays(2))
                .deliveryPriceList("dpl-7")
                .weight(new BigDecimal("1.250"))
                .obligatoryIdentifier("obligatory-1")
                .voluntaryIdentifier("voluntary-1")
                .returnIdentifier("return-1")
                .invoiceType(InvoiceType.VAT_INVOICE)
                .taxRate(TaxRate.TAX_23)
                .basketLimit(5)
                .energyLabel("energy-label-url")
                .instructionWithSafetyInformation("safety-url")
                .informationCard("info-card-url")
                .producerId(99)
                .responsiblePersonId(88)
                .externalMetaProductId("meta-1")
                .automaticDiscountRuleId(11)
                .importantFeatures(List.of("waterproof", "warm"))
                .images(List.of(ProductImage.of("https://example.com/cover.jpg")));
    }

    @Test
    void mapsEveryScalarFieldToTheRawCreateRequest() {
        ProductCreate raw = ProductRequestMapper.toCreate(ProductDraft.of(everyScalarField().build()));

        assertEquals("Kurtka zimowa", raw.getName());
        assertEquals("5901234123457", raw.getEan());
        assertEquals("SKU-9001", raw.getSku());
        assertEquals("pl", raw.getBaseMarket().getValue());
        assertEquals(4242, raw.getSourceFulfillmentProductId());
        assertEquals(17, raw.getStock());
        // Money is sent as an integer count of minor units: 120.50 PLN is 12050 grosze.
        assertEquals(12050, raw.getPrice());
        assertEquals(11800, raw.getMobilePrice());
        assertEquals(15000, raw.getCataloguePrice());
        assertEquals("cataloguePrice", raw.getReferencePriceType().getValue());
        assertEquals("active", raw.getStatus().getValue());
        assertEquals(Boolean.FALSE, raw.getArchived());
        assertEquals("dpl-7", raw.getDeliveryPriceList());
        assertEquals(new BigDecimal("1.250"), raw.getWeight());
        assertEquals("obligatory-1", raw.getObligatoryIdentifier());
        assertEquals("voluntary-1", raw.getVoluntaryIdentifier());
        assertEquals("return-1", raw.getReturnIdentifier());
        assertEquals("vatInvoice", raw.getInvoiceType().getValue());
        assertEquals("TAX_23", raw.getTaxRate().getValue());
        assertEquals(5, raw.getBasketLimit());
        assertEquals("energy-label-url", raw.getEnergyLabel());
        assertEquals("safety-url", raw.getInstructionWithSafetyInformation());
        assertEquals("info-card-url", raw.getInformationCard());
        assertEquals(99, raw.getProducerId());
        assertEquals(88, raw.getResponsiblePersonId());
        assertEquals("meta-1", raw.getExternalMetaProductId());
        assertEquals(11, raw.getAutomaticDiscountRuleId());
        assertEquals(List.of("waterproof", "warm"), raw.getImportantFeatures());
    }

    @Test
    void mapsScalarFieldsToTheRawUpdateRequest() {
        ProductPatch patch = ProductPatch.builder().content(everyScalarField().build()).build();

        ProductUpdate raw = ProductRequestMapper.toUpdate(patch);

        assertEquals("Kurtka zimowa", raw.getName());
        assertEquals("pl", raw.getBaseMarket().getValue());
        assertEquals(12050, raw.getPrice());
        assertEquals("active", raw.getStatus().getValue());
        assertEquals("TAX_23", raw.getTaxRate().getValue());
        assertEquals(17, raw.getStock());
        assertEquals(5, raw.getBasketLimit());
    }

    @Test
    void sendsAnExplicitNullForEachClearedField() {
        ProductPatch patch = ProductPatch.builder()
                .content(ProductContent.builder().stock(9).build())
                .clear(ProductField.MOBILE_PRICE)
                .clear(ProductField.DESCRIPTION)
                .build();

        ProductUpdate raw = ProductRequestMapper.toUpdate(patch);

        assertEquals(9, raw.getStock());
        // A cleared field is carried as an explicit null so the API unsets it; an unset field is absent.
        assertNull(raw.getMobilePrice());
        assertNull(raw.getDescription());
    }

    @Test
    void carriesANewExternalIdOnTheRawUpdate() {
        ProductPatch patch = ProductPatch.builder()
                .content(ProductContent.builder().stock(1).build())
                .newExternalId(ProductExternalId.of("renamed-1"))
                .build();

        ProductUpdate raw = ProductRequestMapper.toUpdate(patch);

        assertEquals("renamed-1", raw.getNewExternalId());
    }

    @Test
    void carriesTheExternalIdAndPayloadInABatchEntry() {
        ProductPatch patch = ProductPatch.builder()
                .content(ProductContent.builder().stock(3).price(Money.ofPln("10.00")).build())
                .build();

        var raw = ProductRequestMapper.toBatchEntry(ProductExternalId.of("batch-7"), patch);

        assertEquals("batch-7", raw.getExternalId());
        assertEquals(3, raw.getStock());
        assertEquals(1000, raw.getPrice());
    }

    @Test
    void leavesOptionalFieldsUnsetOnAMinimalCreate() {
        // The create-required minimum is name + price + stock + dispatchTime + images.
        ProductContent minimal = ProductContent.builder()
                .name("Only the required fields")
                .price(Money.ofPln("10.00"))
                .stock(1)
                .dispatchTime(DispatchTime.ofDays(1))
                .images(List.of(ProductImage.of("https://example.com/cover.jpg")))
                .build();

        ProductCreate raw = ProductRequestMapper.toCreate(ProductDraft.of(minimal));

        assertEquals("Only the required fields", raw.getName());
        assertEquals(1000, raw.getPrice());
        assertEquals(1, raw.getStock());
        // Optional fields the caller did not set are left absent, not sent as null.
        assertNull(raw.getEan());
        assertNull(raw.getBasketLimit());
        assertTrue(raw.getTaxRate() == null);
    }
}
