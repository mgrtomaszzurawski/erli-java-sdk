package io.github.mgrtomaszzurawski.erli.domain.campaigns;

/**
 * Identifier of an Erli ad campaign, shaped {@code <n>-company-<n>-shop-<name>}.
 *
 * <p>Owned by this bucket rather than {@code core}: no other domain references a campaign, so there
 * is no shared type to reuse. Treated as opaque — the SDK never parses the embedded company or shop.
 *
 * @param value the non-blank campaign id
 */
public record CampaignId(String value) {

    public CampaignId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CampaignId must not be null or blank");
        }
        value = value.trim();
    }

    public static CampaignId of(String value) {
        return new CampaignId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
