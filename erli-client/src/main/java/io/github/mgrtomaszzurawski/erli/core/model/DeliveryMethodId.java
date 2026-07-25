package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Identifier of a delivery method. Owned by core; shared by Dictionaries and Shipping &amp; Delivery.
 *
 * @param value the non-blank delivery-method id
 */
public record DeliveryMethodId(String value) {

    public DeliveryMethodId {
        value = Identifiers.requireText(value, "DeliveryMethodId");
    }

    public static DeliveryMethodId of(String value) {
        return new DeliveryMethodId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
