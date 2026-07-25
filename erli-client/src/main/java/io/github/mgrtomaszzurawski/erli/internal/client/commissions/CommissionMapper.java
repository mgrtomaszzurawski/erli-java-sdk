package io.github.mgrtomaszzurawski.erli.internal.client.commissions;

import io.github.mgrtomaszzurawski.erli.core.model.CategoryId;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimate;
import io.github.mgrtomaszzurawski.erli.domain.commissions.CommissionEstimateRequest;
import io.github.mgrtomaszzurawski.erli.internal.client.finance.MinorUnits;
import io.github.mgrtomaszzurawski.erli.rest.model.EstimateCommissionRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.EstimateCommissionResponse;

import java.util.Objects;

/**
 * Translates between the {@code commissions} domain records and the generated Layer-1 models. Kept
 * internal so no {@code *Raw} type reaches an exported signature. Internal.
 */
final class CommissionMapper {

    private CommissionMapper() {
    }

    /** Domain request to the wire model. Prices travel as whole grosze. */
    static EstimateCommissionRequest toRaw(CommissionEstimateRequest request) {
        Objects.requireNonNull(request, "request");
        return new EstimateCommissionRequest()
                .categoryId(toCategoryNumber(request.categoryId()))
                .quantity(request.quantity())
                .unitPrice(MinorUnits.toGrosze(request.unitPrice(), "unitPrice"));
    }

    /** Wire response to the domain record. */
    static CommissionEstimate toDomain(EstimateCommissionResponse rawResponse) {
        Objects.requireNonNull(rawResponse, "raw EstimateCommissionResponse");
        Integer commission = rawResponse.getCommission();
        if (commission == null) {
            throw new IllegalStateException(
                    "EstimateCommissionResponse is missing the required 'commission' field");
        }
        return new CommissionEstimate(MinorUnits.fromGrosze(commission));
    }

    /**
     * Erli category ids are numeric, but core models {@link CategoryId} as an opaque text identifier
     * so every bucket shares one type. Convert here, at the wire boundary, with a clear failure.
     */
    private static Integer toCategoryNumber(CategoryId categoryId) {
        try {
            return Integer.valueOf(categoryId.value());
        } catch (NumberFormatException notNumeric) {
            throw new IllegalArgumentException(
                    "categoryId must be numeric for the commissions API, got '"
                            + categoryId.value() + "'", notNumeric);
        }
    }
}
