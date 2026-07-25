package io.github.mgrtomaszzurawski.erli.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * How an order is shipped.
 *
 * @param name           the delivery method's name as shown to the buyer
 * @param typeId         the delivery method's id
 * @param price          what the buyer paid for delivery
 * @param cancelledPrice the cancelled portion of the delivery charge, when the API reports one. The
 *                       API sends this as a bare integer beside {@code price}; both are minor currency
 *                       units, so the SDK models it as {@link Money} for parity with {@link #price()}
 *                       rather than leaking the raw integer. See KNOWN-SERVER-BEHAVIORS (the field is
 *                       undocumented in the spec; this is an inferred semantic)
 * @param cashOnDelivery whether the buyer pays on delivery
 * @param sourceMarket   the market the order originates from, when the API reports it
 * @param targetMarket   the market the order is delivered to, when the API reports it
 * @param pickupPlace    the chosen pickup point, for pickup delivery methods
 */
public record Delivery(
        String name,
        DeliveryMethodId typeId,
        Money price,
        Optional<Money> cancelledPrice,
        boolean cashOnDelivery,
        Optional<String> sourceMarket,
        Optional<String> targetMarket,
        Optional<PickupPlace> pickupPlace) {

    public Delivery {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(cancelledPrice, "cancelledPrice");
        Objects.requireNonNull(sourceMarket, "sourceMarket");
        Objects.requireNonNull(targetMarket, "targetMarket");
        Objects.requireNonNull(pickupPlace, "pickupPlace");
    }
}
