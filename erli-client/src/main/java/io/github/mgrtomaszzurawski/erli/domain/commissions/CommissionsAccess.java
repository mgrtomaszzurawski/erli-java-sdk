package io.github.mgrtomaszzurawski.erli.domain.commissions;

import io.github.mgrtomaszzurawski.erli.core.error.ErliApiException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;

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
     * <p>The result is an estimate for the current day, not a quote: commission rates can change.
     *
     * @param request the category, unit price and quantity to price
     * @return the estimated commission for the whole quantity
     * @throws ErliValidationException  if the API rejects the request — most often because the
     *                                  category is not a leaf category
     * @throws ErliApiException         for any other error the API reports (authentication,
     *                                  not-found, server failure)
     * @throws ErliTransportException   if the request could not be completed or decoded
     * @throws IllegalArgumentException if the unit price is not a whole number of grosze in PLN, or
     *                                  the category id is not numeric
     */
    CommissionEstimate estimate(CommissionEstimateRequest request);
}
