package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.util.Objects;

/**
 * A delivery price list as it appears in the index — id and name only. Fetch the priced entries with
 * {@link DeliveryAccess#priceListDetails(PriceListQuery)}.
 *
 * @param id   the price list's numeric id
 * @param name its unique name; the shop's default list is named {@code "*"}
 */
public record PriceListSummary(long id, String name) {

    public PriceListSummary {
        Objects.requireNonNull(name, "name");
    }
}
