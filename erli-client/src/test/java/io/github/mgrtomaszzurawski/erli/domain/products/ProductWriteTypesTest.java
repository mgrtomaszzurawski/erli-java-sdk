package io.github.mgrtomaszzurawski.erli.domain.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.core.model.SortOrder;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Rules the write-side domain types enforce locally, so a request the marketplace would reject fails at
 * the call site with a message naming the problem instead of after a round trip.
 *
 * <p>These need no transport and no JSON, so they are the honest floor of the bucket's behaviour.
 */
class ProductWriteTypesTest {

    private static final String COVER_IMAGE = "https://example.com/cover.jpg";

    private static ProductContent.Builder completeContent() {
        return ProductContent.builder()
                .name("Kurtka zimowa")
                .price(Money.ofPln("100.00"))
                .stock(10)
                .dispatchTime(DispatchTime.ofDays(1))
                .images(List.of(ProductImage.of(COVER_IMAGE)));
    }

    @Test
    void acceptsADraftCarryingEveryFieldRequiredOnCreate() {
        ProductDraft draft = ProductDraft.of(completeContent().build());
        assertEquals("Kurtka zimowa", draft.content().name().orElseThrow());
        assertEquals(10, draft.content().stock().orElseThrow());
    }

    @Test
    void namesEveryMissingRequiredFieldAtOnce() {
        ProductContent nameOnlyContent = ProductContent.builder().name("Only a name").build();
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ProductDraft.of(nameOnlyContent));
        String message = failure.getMessage();
        assertTrue(message.contains("price"), message);
        assertTrue(message.contains("stock"), message);
        assertTrue(message.contains("dispatchTime"), message);
        assertTrue(message.contains("images"), message);
        assertFalse(message.contains("name"), message);
    }

    @Test
    void treatsAnEmptyImageListAsAMissingCover() {
        ProductContent contentWithoutImages = completeContent().images(List.of()).build();
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> ProductDraft.of(contentWithoutImages));
        assertTrue(failure.getMessage().contains("images"), failure.getMessage());
    }

    @Test
    void keepsSetAndClearedFieldsApartOnAPatch() {
        ProductPatch patch = ProductPatch.builder()
                .content(ProductContent.builder().stock(5).build())
                .clear(ProductField.MOBILE_PRICE, ProductField.EAN)
                .build();
        assertEquals(5, patch.content().stock().orElseThrow());
        assertTrue(patch.content().name().isEmpty());
        assertEquals(java.util.Set.of(ProductField.MOBILE_PRICE, ProductField.EAN), patch.cleared());
    }

    @Test
    void refusesToClearAFieldTheApiDoesNotAllowRemoving() {
        var patchBuilder = ProductPatch.builder();
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> patchBuilder.clear(ProductField.NAME));
        assertTrue(failure.getMessage().contains("NAME"), failure.getMessage());
    }

    @Test
    void rejectsAnOrderedComparisonOnAnEqualityOnlyField() {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> ProductFilter.greaterThan(ProductFilterField.STATUS, "active"));
        assertTrue(failure.getMessage().contains("STATUS"), failure.getMessage());
        // The same field is fine with equality.
        assertTrue(ProductFilter.equalTo(ProductFilterField.STATUS, "active")
                instanceof ProductFilter.Comparison);
    }

    @Test
    void allowsOrderedComparisonsOnScalarFields() {
        ProductFilter.Comparison filter = (ProductFilter.Comparison)
                ProductFilter.atLeast(ProductFilterField.STOCK, "1");
        assertEquals(ComparisonOperator.GREATER_OR_EQUAL, filter.operator());
        assertEquals("1", filter.value());
    }

    @Test
    void rejectsAnEmptyMembershipSetAndAnEmptyJunction() {
        List<String> emptyMembership = List.of();
        assertThrows(IllegalArgumentException.class,
                () -> ProductFilter.in(ProductFilterField.SKU, emptyMembership));
        assertThrows(IllegalArgumentException.class, ProductFilter::and);
    }

    @Test
    void rejectsAPageSizeOutsideTheRangeErliAccepts() {
        var zeroPageBuilder = ProductSearchRequest.builder();
        assertThrows(IllegalArgumentException.class, () -> zeroPageBuilder.pageSize(0));
        var oversizePageBuilder = ProductSearchRequest.builder();
        assertThrows(IllegalArgumentException.class,
                () -> oversizePageBuilder.pageSize(ProductSearchRequest.MAX_PAGE_SIZE + 1));
        assertEquals(ProductSearchRequest.MAX_PAGE_SIZE, ProductSearchRequest.builder()
                .pageSize(ProductSearchRequest.MAX_PAGE_SIZE).build().pageSize().orElseThrow());
    }

    @Test
    void defaultsASearchToExternalIdAscending() {
        ProductSearchRequest request = ProductSearchRequest.all();
        assertEquals(ProductSortField.EXTERNAL_ID, request.sortField());
        assertEquals(SortOrder.ASCENDING, request.order());
        assertTrue(request.filter().isEmpty());
        assertTrue(request.fields().isEmpty());
    }

    @Test
    void rejectsADiscountThatEndsBeforeItStarts() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-01T00:00:00+02:00");
        OffsetDateTime end = OffsetDateTime.parse("2026-07-01T00:00:00+02:00");
        Money discountAmount = Money.ofPln("10.00");
        assertThrows(IllegalArgumentException.class,
                () -> DiscountRequest.between(discountAmount, start, end));
    }

    @Test
    void reportsWhetherADiscountIsRunningAtAnInstant() {
        OffsetDateTime start = OffsetDateTime.parse("2026-08-01T00:00:00+02:00");
        OffsetDateTime end = OffsetDateTime.parse("2026-08-08T00:00:00+02:00");
        Discount discount = new Discount(
                io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId.of("sku-1"),
                100007L, Money.ofPln("10.00"), start, end, true);
        assertTrue(discount.isActiveAt(start));
        assertTrue(discount.isActiveAt(OffsetDateTime.parse("2026-08-04T12:00:00+02:00")));
        assertFalse(discount.isActiveAt(end));
        assertFalse(discount.isActiveAt(start.minusDays(1)));
    }

    @Test
    void appliesTheWorkingDayDefaultAndRejectsANegativePeriod() {
        assertEquals(DispatchTimeUnit.DAY, new DispatchTime(null, 3).unit());
        assertEquals(3, DispatchTime.ofDays(3).period());
        assertEquals(DispatchTimeUnit.HOUR, DispatchTime.ofHours(24).unit());
        assertThrows(IllegalArgumentException.class, () -> DispatchTime.ofDays(-1));
    }

    @Test
    void answersWhetherAFieldIsPinned() {
        FrozenFields frozen = FrozenFields.of(java.util.Set.of(ProductField.PRICE));
        assertTrue(frozen.isFrozen(ProductField.PRICE));
        assertFalse(frozen.isFrozen(ProductField.STOCK));
        assertFalse(frozen.isEmpty());
        assertTrue(FrozenFields.none().isEmpty());
    }
}
