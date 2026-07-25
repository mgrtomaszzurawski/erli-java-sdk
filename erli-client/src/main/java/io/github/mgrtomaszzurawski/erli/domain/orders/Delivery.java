package io.github.mgrtomaszzurawski.erli.domain.orders;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * How the order is to be delivered and what the buyer paid for it.
 *
 * @param name         the delivery method's display name
 * @param typeId       the delivery method, as listed by {@code GET /dictionaries/deliveryMethods}
 * @param price        the delivery charge in the order's currency
 * @param cancelled    the number of cancelled parcels, when Erli reports it
 * @param cashOnDelivery whether the buyer pays the courier on delivery
 * @param sourceMarket the market the order was placed on, when Erli reports it
 * @param targetMarket the market the order is delivered to, when Erli reports it
 * @param pickupPlace  the chosen collection point, for pickup-style deliveries
 */
public record Delivery(
        String name,
        DeliveryMethodId typeId,
        Money price,
        OptionalInt cancelled,
        boolean cashOnDelivery,
        Optional<String> sourceMarket,
        Optional<String> targetMarket,
        Optional<PickupPlace> pickupPlace) {
}
