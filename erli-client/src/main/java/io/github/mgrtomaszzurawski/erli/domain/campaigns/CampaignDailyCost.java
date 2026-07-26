package io.github.mgrtomaszzurawski.erli.domain.campaigns;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * What one campaign cost the shop on one day.
 *
 * <p>Only {@link #netShopCost()} is guaranteed by the API; a row that aggregates across campaigns,
 * or one the API reports without a date, leaves the other two empty.
 *
 * @param campaignId  the campaign this cost belongs to, if the API attributed it to one
 * @param date        the day the cost was incurred, if the API reported one
 * @param netShopCost the net cost charged to the shop (excluding VAT)
 */
public record CampaignDailyCost(
        Optional<CampaignId> campaignId,
        Optional<OffsetDateTime> date,
        Money netShopCost) {
}
