package io.github.mgrtomaszzurawski.erli.internal.client.campaigns;

import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignCostSummary;
import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignDailyCost;
import io.github.mgrtomaszzurawski.erli.domain.campaigns.CampaignId;
import io.github.mgrtomaszzurawski.erli.internal.client.finance.MinorUnits;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopCampaignsCostSummaryResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopCampaignsCostSummaryResponseDataInner;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated campaign-summary models to the {@code campaigns} domain records. Internal.
 */
final class CampaignMapper {

    private static final String FIELD_SHOP_ID = "shopId";
    private static final String FIELD_NET_SHOP_COST = "netShopCost";
    private static final String RAW_RESPONSE_NAME = "raw ShopCampaignsCostSummaryResponse";

    private CampaignMapper() {
    }

    static CampaignCostSummary toDomain(ShopCampaignsCostSummaryResponse rawSummary) {
        Objects.requireNonNull(rawSummary, RAW_RESPONSE_NAME);
        return new CampaignCostSummary(requireShopId(rawSummary), toDailyCosts(rawSummary.getData()));
    }

    private static long requireShopId(ShopCampaignsCostSummaryResponse rawSummary) {
        BigDecimal shopId = rawSummary.getShopId();
        if (shopId == null) {
            throw new IllegalStateException(
                    "ShopCampaignsCostSummaryResponse is missing the required '" + FIELD_SHOP_ID + "' field");
        }
        return shopId.longValueExact();
    }

    private static List<CampaignDailyCost> toDailyCosts(
            List<ShopCampaignsCostSummaryResponseDataInner> rawRows) {
        if (rawRows == null) {
            return List.of();
        }
        return rawRows.stream().map(CampaignMapper::toDailyCost).toList();
    }

    private static CampaignDailyCost toDailyCost(ShopCampaignsCostSummaryResponseDataInner rawRow) {
        BigDecimal netShopCost = rawRow.getNetShopCost();
        if (netShopCost == null) {
            throw new IllegalStateException(
                    "Campaign cost row is missing the required '" + FIELD_NET_SHOP_COST + "' field");
        }
        return new CampaignDailyCost(
                Optional.ofNullable(rawRow.getCampaignId()).map(CampaignId::of),
                Optional.ofNullable(rawRow.getDate()),
                // Documented by the spec as "koszty netto (w groszach)" — net cost in grosze.
                MinorUnits.fromGroszeAmount(netShopCost));
    }
}
