package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A new delivery price list to create. Build one with {@link #builder(String)}.
 *
 * <p>The two flags default to {@code false}, matching the API's behaviour for an omitted value, so a
 * caller only names what they want switched on.
 *
 * @param name                   the unique list name; {@code "*"} replaces the shop's default list
 * @param prices                 the priced delivery methods, at least one
 * @param erliProEnabled         join the ErliPRO free-delivery programme
 * @param nextDayDeliveryEnabled enable next-day delivery for products on this list
 */
public record PriceListDraft(
        String name, List<DeliveryPrice> prices, boolean erliProEnabled, boolean nextDayDeliveryEnabled) {

    public PriceListDraft {
        Objects.requireNonNull(name, "name");
        prices = List.copyOf(Objects.requireNonNull(prices, "prices"));
        if (prices.isEmpty()) {
            throw new IllegalArgumentException("a price list must carry at least one delivery price");
        }
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** Builder for {@link PriceListDraft}. */
    public static final class Builder {

        private final String name;
        private final List<DeliveryPrice> prices = new ArrayList<>();
        private boolean erliProEnabled;
        private boolean nextDayDeliveryEnabled;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        /** Add one priced delivery method. At least one is required. */
        public Builder price(DeliveryPrice deliveryPrice) {
            prices.add(Objects.requireNonNull(deliveryPrice, "deliveryPrice"));
            return this;
        }

        public Builder erliProEnabled(boolean value) {
            this.erliProEnabled = value;
            return this;
        }

        public Builder nextDayDeliveryEnabled(boolean value) {
            this.nextDayDeliveryEnabled = value;
            return this;
        }

        public PriceListDraft build() {
            return new PriceListDraft(name, prices, erliProEnabled, nextDayDeliveryEnabled);
        }
    }
}
