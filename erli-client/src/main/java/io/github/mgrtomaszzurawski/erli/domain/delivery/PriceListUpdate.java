package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Replacement content for an existing price list. Build one with {@link #builder()}.
 *
 * <p>This is a <em>replacement</em>, not a merge: the API requires the full price set on every update,
 * so a method omitted here is removed from the list. The name cannot be changed this way.
 *
 * @param prices                 the complete new price set, at least one entry
 * @param erliProEnabled         join the ErliPRO free-delivery programme
 * @param nextDayDeliveryEnabled enable next-day delivery for products on this list
 */
public record PriceListUpdate(
        List<DeliveryPrice> prices, boolean erliProEnabled, boolean nextDayDeliveryEnabled) {

    public PriceListUpdate {
        prices = List.copyOf(Objects.requireNonNull(prices, "prices"));
        if (prices.isEmpty()) {
            throw new IllegalArgumentException("a price list must carry at least one delivery price");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link PriceListUpdate}. */
    public static final class Builder {

        private final List<DeliveryPrice> prices = new ArrayList<>();
        private boolean erliProEnabled;
        private boolean nextDayDeliveryEnabled;

        private Builder() {
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

        public PriceListUpdate build() {
            return new PriceListUpdate(prices, erliProEnabled, nextDayDeliveryEnabled);
        }
    }
}
