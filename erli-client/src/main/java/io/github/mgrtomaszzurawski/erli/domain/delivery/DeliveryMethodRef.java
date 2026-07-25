package io.github.mgrtomaszzurawski.erli.domain.delivery;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;

import java.util.Objects;
import java.util.Optional;

/**
 * The delivery method a price entry applies to, plus the window it promises.
 *
 * <p>Identified by the core {@link DeliveryMethodId} rather than an SDK enum: the spec lists over a
 * hundred method ids and grows with every carrier deal, and the Dictionaries bucket already exposes the
 * authoritative list keyed by the same id.
 *
 * @param id           the delivery method
 * @param deliveryTime the promised window, when the price list states one
 */
public record DeliveryMethodRef(DeliveryMethodId id, Optional<DeliveryTime> deliveryTime) {

    public DeliveryMethodRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(deliveryTime, "deliveryTime");
    }
}
