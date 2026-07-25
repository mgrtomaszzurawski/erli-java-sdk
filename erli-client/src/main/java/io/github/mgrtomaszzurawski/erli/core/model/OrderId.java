package io.github.mgrtomaszzurawski.erli.core.model;

/**
 * Identifier of an Erli order. Owned by core so the Orders bucket and any cross-referencing bucket
 * (shipping, payments) share one type instead of minting private copies.
 *
 * @param value the non-blank order id
 */
public record OrderId(String value) {

    public OrderId {
        value = Identifiers.requireText(value, "OrderId");
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
