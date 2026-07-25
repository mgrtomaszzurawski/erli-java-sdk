package io.github.mgrtomaszzurawski.erli.domain.campaigns;

import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.util.List;

/**
 * Ad-campaign spend for one shop over a requested date range, broken down by day and campaign.
 *
 * @param shopId     the shop the costs belong to
 * @param dailyCosts one row per campaign per day; empty when nothing was spent in the range
 */
public record CampaignCostSummary(long shopId, List<CampaignDailyCost> dailyCosts) {

    public CampaignCostSummary {
        dailyCosts = List.copyOf(dailyCosts);
    }

    /** Total net spend across every row, useful for a whole-range figure without streaming. */
    public Money totalNetCost() {
        return dailyCosts.stream()
                .map(CampaignDailyCost::netShopCost)
                .reduce(Money.ofPln("0.00"), (left, right) ->
                        new Money(left.amount().add(right.amount()), right.currency()));
    }
}
