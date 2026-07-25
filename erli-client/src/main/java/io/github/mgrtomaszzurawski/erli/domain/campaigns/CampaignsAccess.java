package io.github.mgrtomaszzurawski.erli.domain.campaigns;

import io.github.mgrtomaszzurawski.erli.core.error.ErliApiException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import java.time.LocalDate;

/**
 * Ad-campaign spend. Reached via {@code client.campaigns()}.
 */
public interface CampaignsAccess {

    /**
     * Summarize what the shop's campaigns cost over a date range
     * ({@code GET /campaigns/campaigns-summary}).
     *
     * <p>Both dates are <strong>required</strong> and both bounds are inclusive dates, not instants.
     * The API also refuses an {@code endDate} in the future — it must not be past the marketplace's
     * current time, so "today" can still be rejected early in the day depending on the server clock.
     * (The published spec marks both parameters optional; the live API does not. See
     * {@code KNOWN-SERVER-BEHAVIORS.md}.)
     *
     * @param startDate first day to include
     * @param endDate   last day to include; must not be in the future
     * @return the per-day, per-campaign cost breakdown
     * @throws ErliValidationException if a date is missing or {@code endDate} is in the future
     * @throws ErliApiException        for any other error the API reports
     * @throws ErliTransportException  if the request could not be completed or decoded
     */
    CampaignCostSummary costSummary(LocalDate startDate, LocalDate endDate);
}
