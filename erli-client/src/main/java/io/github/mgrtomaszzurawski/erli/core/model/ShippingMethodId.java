package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Identifier of a shipping method. Owned by core; shared by Dictionaries and Shipping &amp; Delivery.
 *
 * @param value the non-blank shipping-method id
 */
public record ShippingMethodId(String value) {

    public ShippingMethodId {
        value = Identifiers.requireText(value, "ShippingMethodId");
    }

    public static ShippingMethodId of(String value) {
        return new ShippingMethodId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
