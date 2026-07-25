package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A delivery price list: the set of methods a shop offers and what each costs.
 *
 * <p>A shop can keep several lists and attach different ones to different products; the list named
 * {@code "*"} is the default. Prices are never empty — the API requires at least one entry.
 *
 * @param id                     the list's numeric id
 * @param name                   its unique name; {@code "*"} for the default list
 * @param prices                 the priced delivery methods, at least one
 * @param erliProEnabled         whether products on this list join the ErliPRO free-delivery programme
 * @param nextDayDeliveryEnabled whether next-day delivery is enabled for products on this list
 * @param createdAt              when the list was created
 * @param updatedAt              when it last changed, once it has
 */
public record PriceList(
        long id,
        String name,
        List<DeliveryPrice> prices,
        boolean erliProEnabled,
        boolean nextDayDeliveryEnabled,
        OffsetDateTime createdAt,
        Optional<OffsetDateTime> updatedAt) {

    public PriceList {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        prices = List.copyOf(Objects.requireNonNull(prices, "prices"));
    }
}
