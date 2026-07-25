package io.github.mgrtomaszzurawski.erli.internal.client.campaigns;

import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignCostSummary;
import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignsAccess;
import io.github.mgrtomaszzurawski.erli.internal.ApiPaths;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.client.finance.QueryString;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopCampaignsCostSummaryResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * {@link CampaignsAccess} implementation over the shared {@link HttpRuntime}. Internal.
 */
public final class CampaignsAccessImpl implements CampaignsAccess {

    private static final String START_DATE_PARAMETER = "startDate";
    private static final String END_DATE_PARAMETER = "endDate";

    /** The API documents and accepts {@code yyyy-mm-dd} for both bounds. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final HttpRuntime runtime;

    public CampaignsAccessImpl(HttpRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public CampaignCostSummary costSummary(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, START_DATE_PARAMETER);
        Objects.requireNonNull(endDate, END_DATE_PARAMETER);
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "endDate " + endDate + " must not be before startDate " + startDate);
        }
        String query = new QueryString()
                .add(START_DATE_PARAMETER, DATE_FORMAT.format(startDate))
                .add(END_DATE_PARAMETER, DATE_FORMAT.format(endDate))
                .render();
        ShopCampaignsCostSummaryResponse rawResponse = runtime.get(ApiPaths.CAMPAIGNS_SUMMARY + query, ShopCampaignsCostSummaryResponse.class);
        return CampaignMapper.toDomain(rawResponse);
    }
}
