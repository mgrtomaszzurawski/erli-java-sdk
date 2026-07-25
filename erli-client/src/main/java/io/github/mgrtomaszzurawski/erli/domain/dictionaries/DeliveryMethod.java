package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;

import java.util.Objects;

/**
 * A delivery method offered by the marketplace — one of the values that may appear as
 * {@code Order.delivery}. Reference data, identical for every shop.
 *
 * @param id the delivery-method identifier, e.g. {@code erliPaczkomat}
 * @param name the human-readable Polish label shown to buyers
 * @param cashOnDelivery whether this method supports cash on delivery (COD)
 * @param vendor the carrier operating the method
 */
public record DeliveryMethod(DeliveryMethodId id, String name, boolean cashOnDelivery, DeliveryVendor vendor) {

    public DeliveryMethod {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(vendor, "vendor");
    }
}
