package io.github.mgrtomaszzurawski.erli.internal.client.commissions;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimate;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.EstimateCommissionRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.EstimateCommissionResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Field-level mapper coverage, asserted directly rather than through the facade (TESTING.md). */
class CommissionMapperTest {

    private static final String LEAF_CATEGORY_ID = "4";
    private static final String UNIT_PRICE_PLN = "100.00";

    @Test
    void writesEveryRequestFieldOntoTheWireModel() {
        EstimateCommissionRequest raw = CommissionMapper.toRaw(CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                .unitPrice(Money.ofPln(UNIT_PRICE_PLN))
                .quantity(7)
                .build());

        assertEquals(4, raw.getCategoryId());
        assertEquals(7, raw.getQuantity());
        assertEquals(10000, raw.getUnitPrice());
    }

    @Test
    void defaultsQuantityToOneWhenTheCallerDoesNotSetIt() {
        EstimateCommissionRequest raw = CommissionMapper.toRaw(CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of(LEAF_CATEGORY_ID))
                .unitPrice(Money.ofPln(UNIT_PRICE_PLN))
                .build());

        assertEquals(CommissionEstimateRequest.DEFAULT_QUANTITY, raw.getQuantity());
    }

    @Test
    void rejectsANonNumericCategoryId() {
        CommissionEstimateRequest request = CommissionEstimateRequest.builder()
                .categoryId(CategoryId.of("electronics"))
                .unitPrice(Money.ofPln(UNIT_PRICE_PLN))
                .build();

        IllegalArgumentException failure =
                assertThrows(IllegalArgumentException.class, () -> CommissionMapper.toRaw(request));

        assertTrue(failure.getMessage().contains("categoryId"), failure.getMessage());
    }

    @Test
    void readsTheCommissionAsMoney() {
        CommissionEstimate estimate =
                CommissionMapper.toDomain(new EstimateCommissionResponse().commission(1168));

        assertEquals(Money.ofPln("11.68"), estimate.commission());
    }

    @Test
    void acceptsAZeroCommission() {
        // The spec allows minimum 0, so a commission-free category must not be treated as an error.
        assertEquals(Money.ofPln("0.00"),
                CommissionMapper.toDomain(new EstimateCommissionResponse().commission(0)).commission());
    }

    @Test
    void rejectsAResponseMissingTheRequiredCommission() {
        EstimateCommissionResponse raw = new EstimateCommissionResponse();

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> CommissionMapper.toDomain(raw));

        assertTrue(failure.getMessage().contains("commission"), failure.getMessage());
    }

    @Test
    void rejectsANegativeCommission() {
        EstimateCommissionResponse raw = new EstimateCommissionResponse().commission(-1);

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> CommissionMapper.toDomain(raw));

        assertTrue(failure.getMessage().contains("negative"), failure.getMessage());
    }
}
