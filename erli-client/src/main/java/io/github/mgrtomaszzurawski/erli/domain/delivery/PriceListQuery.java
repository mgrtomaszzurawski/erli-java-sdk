package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Filters for {@link DeliveryAccess#priceListDetails(PriceListQuery)}. Every filter is optional;
 * {@link #none()} fetches every list. Ids and names accumulate — the API treats repeats as "any of".
 *
 * @param ids            match these price-list ids
 * @param names          match these price-list names
 * @param erliProEnabled restrict to lists with ErliPRO on or off
 */
public record PriceListQuery(List<Long> ids, List<String> names, Optional<Boolean> erliProEnabled) {

    private static final PriceListQuery NONE = new PriceListQuery(List.of(), List.of(), Optional.empty());

    public PriceListQuery {
        Objects.requireNonNull(erliProEnabled, "erliProEnabled");
        ids = List.copyOf(Objects.requireNonNull(ids, "ids"));
        names = List.copyOf(Objects.requireNonNull(names, "names"));
    }

    /** No filters — every price list the shop has. */
    public static PriceListQuery none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link PriceListQuery}. */
    public static final class Builder {

        private final List<Long> ids = new ArrayList<>();
        private final List<String> names = new ArrayList<>();
        private Boolean erliProEnabled;

        private Builder() {
        }

        public Builder id(long priceListId) {
            ids.add(priceListId);
            return this;
        }

        public Builder name(String priceListName) {
            names.add(Objects.requireNonNull(priceListName, "priceListName"));
            return this;
        }

        public Builder erliProEnabled(boolean value) {
            this.erliProEnabled = value;
            return this;
        }

        public PriceListQuery build() {
            return new PriceListQuery(ids, names, Optional.ofNullable(erliProEnabled));
        }
    }
}
