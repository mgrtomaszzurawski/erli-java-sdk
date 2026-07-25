package io.github.mgrtomaszzurawski.erli.domain.delivery;

import io.github.mgrtomaszzurawski.erli.core.model.Money;
import java.util.Objects;
import java.util.Optional;

/**
 * What one delivery method costs under a price list.
 *
 * <p>The API states both amounts in grosze; they are exposed as {@link Money} so a caller never has to
 * remember the scale. {@code nextItemPrice} is the surcharge for each additional item in the same
 * parcel, not the total.
 *
 * @param deliveryMethod       the method this price applies to
 * @param basePrice            cost of the first item
 * @param nextItemPrice        surcharge per additional item in the same parcel
 * @param limit                how many items fit in one parcel, when the price list caps it
 * @param nextDayDeliveryOption whether next-day delivery is offered on this method
 */
public record DeliveryPrice(
        DeliveryMethodRef deliveryMethod,
        Money basePrice,
        Money nextItemPrice,
        Optional<PackingLimit> limit,
        boolean nextDayDeliveryOption) {

    public DeliveryPrice {
        Objects.requireNonNull(deliveryMethod, "deliveryMethod");
        Objects.requireNonNull(basePrice, "basePrice");
        Objects.requireNonNull(nextItemPrice, "nextItemPrice");
        Objects.requireNonNull(limit, "limit");
    }
}
