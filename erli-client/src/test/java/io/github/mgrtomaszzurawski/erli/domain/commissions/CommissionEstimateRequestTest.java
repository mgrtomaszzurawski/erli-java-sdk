package io.github.mgrtomaszzurawski.erli.domain.commissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import org.junit.jupiter.api.Test;

/** The public request contract: what a consumer can and cannot build. */
class CommissionEstimateRequestTest {

    private static final String LEAF_CATEGORY_ID = "4";
    private static final String UNIT_PRICE_PLN = "100.00";

    private static CommissionEstimateRequest.Builder validBuilder() {
        return CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                .unitPrice(Money.ofPln(UNIT_PRICE_PLN));
    }

    @Test
    void defaultsQuantityToOne() {
        assertEquals(1, CommissionEstimateRequest.DEFAULT_QUANTITY);
        assertEquals(CommissionEstimateRequest.DEFAULT_QUANTITY, validBuilder().build().quantity());
    }

    @Test
    void keepsWhatTheBuilderWasGiven() {
        CommissionEstimateRequest request = validBuilder().quantity(5).build();

        assertEquals(CategoryId.of(LEAF_CATEGORY_ID), request.categoryId());
        assertEquals(Money.ofPln(UNIT_PRICE_PLN), request.unitPrice());
        assertEquals(5, request.quantity());
    }

    @Test
    void requiresACategory() {
        CommissionEstimateRequest.Builder builder = CommissionEstimateRequest.builder()
                .unitPrice(Money.ofPln(UNIT_PRICE_PLN));

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void requiresAUnitPrice() {
        CommissionEstimateRequest.Builder builder = CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of(LEAF_CATEGORY_ID));

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void rejectsAQuantityBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> validBuilder().quantity(0).build());
        assertThrows(IllegalArgumentException.class, () -> validBuilder().quantity(-1).build());
    }
}
