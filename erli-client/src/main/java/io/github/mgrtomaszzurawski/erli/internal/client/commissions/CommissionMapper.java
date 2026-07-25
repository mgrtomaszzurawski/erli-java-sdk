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

    private static final String FIELD_UNIT_PRICE = "unitPrice";
    private static final String FIELD_COMMISSION = "commission";
    private static final String RAW_RESPONSE_NAME = "raw EstimateCommissionResponse";

    private CommissionMapper() {
    }

    /** Domain request to the wire model. Prices travel as whole grosze. */
    static EstimateCommissionRequest toRaw(CommissionEstimateRequest request) {
        Objects.requireNonNull(request, "request");
        return new EstimateCommissionRequest()
                .categoryId(toCategoryNumber(request.categoryId()))
                .quantity(request.quantity())
                .unitPrice(MinorUnits.toGrosze(request.unitPrice(), FIELD_UNIT_PRICE));
    }

    /** Wire response to the domain record. */
    static CommissionEstimate toDomain(EstimateCommissionResponse rawResponse) {
        Objects.requireNonNull(rawResponse, RAW_RESPONSE_NAME);
        Integer commission = rawResponse.getCommission();
        if (commission == null) {
            throw new IllegalStateException(
                    "EstimateCommissionResponse is missing the required '" + FIELD_COMMISSION + "' field");
        }
        if (commission < 0) {
            // The spec declares minimum 0; a negative commission would silently invert a margin
            // calculation, so fail loudly rather than hand it to the caller.
            throw new IllegalStateException(
                    "EstimateCommissionResponse returned a negative '" + FIELD_COMMISSION + "': " + commission);
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
