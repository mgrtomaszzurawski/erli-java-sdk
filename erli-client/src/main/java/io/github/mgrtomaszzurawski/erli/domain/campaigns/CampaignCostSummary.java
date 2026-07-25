package io.github.mgrtomaszzurawski.erli.domain.campaigns;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

/**
 * Ad-campaign spend for one shop over a requested date range, broken down by day and campaign.
 *
 * @param shopId     the shop the costs belong to
 * @param dailyCosts one row per campaign per day; empty when nothing was spent in the range
 */
public record CampaignCostSummary(long shopId, List<CampaignDailyCost> dailyCosts) {

    /** Identity for {@link #totalNetCost()}; also the answer for a range with no spend. */
    private static final Money ZERO_COST = new Money(BigDecimal.ZERO, Currency.getInstance("PLN"));

    public CampaignCostSummary {
        dailyCosts = List.copyOf(dailyCosts);
    }

    /** Total net spend across every row; zero when the range had no spend. */
    public Money totalNetCost() {
        return dailyCosts.stream()
                .map(CampaignDailyCost::netShopCost)
                .reduce(ZERO_COST, (left, right) ->
                        new Money(left.amount().add(right.amount()), right.currency()));
    }
}
