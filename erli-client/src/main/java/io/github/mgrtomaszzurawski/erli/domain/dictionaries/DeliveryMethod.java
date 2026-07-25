package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;

/**
 * A delivery method from the Erli dictionary — one of the methods that may appear in
 * {@code Order.delivery}. Reference data, shared across shops.
 *
 * @param id             the method identifier (core-owned typed id)
 * @param name           the human-readable method name
 * @param cashOnDelivery whether the method supports cash on delivery (COD)
 * @param vendor         the carrier the method belongs to
 */
public record DeliveryMethod(
        DeliveryMethodId id,
        String name,
        boolean cashOnDelivery,
        DeliveryVendor vendor) {
}
