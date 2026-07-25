package io.github.mgrtomaszzurawski.erli.domain.commissions;

/**
 * Commission estimates. Reached via {@code client.commissions()}.
 *
 * <p>Use it to work out what a listing would cost before creating it — for example to price an item
 * so the margin survives the marketplace fee.
 */
public interface CommissionsAccess {

    /**
     * Estimate today's marketplace commission for a listing ({@code POST /commissions/_estimate}).
     *
     * @param request the category, unit price and quantity to price
     * @return the estimated commission for the whole quantity
     * @throws io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException if the category is
     *                                                                            not a leaf category
     */
    CommissionEstimate estimate(CommissionEstimateRequest request);
}
