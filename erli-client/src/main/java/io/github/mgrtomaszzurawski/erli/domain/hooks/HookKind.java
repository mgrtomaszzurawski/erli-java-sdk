package io.github.mgrtomaszzurawski.erli.domain.hooks;

import io.github.mgrtomaszzurawski.erli.core.model.HookName;

/**
 * The webhook subscriptions Erli can call on a shop. Erli addresses a subscription by its name, and
 * the set of names is closed, so the SDK models it as an enum rather than a free string.
 *
 * <p>Two of them are <em>request/response</em> hooks Erli calls to ask the shop something
 * ({@link #CHECK_BUYABILITY}, {@link #PRODUCTS_NEED_SYNC}); the three {@code ORDER_*} ones are
 * notifications that also arrive as inbox messages.
 */
public enum HookKind {

    /** Erli asks the shop whether given products are still buyable and in what quantity. */
    CHECK_BUYABILITY("checkBuyability"),
    /** Erli tells the shop which products need re-synchronising. */
    PRODUCTS_NEED_SYNC("productsNeedSync"),
    /** A new order was placed. */
    ORDER_CREATED("orderCreated"),
    /** The marketplace-side status of an order changed. */
    ORDER_STATUS_CHANGED("orderStatusChanged"),
    /** The seller-side status of an order changed. */
    ORDER_SELLER_STATUS_CHANGED("orderSellerStatusChanged");

    private final String wireValue;

    HookKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The value the API uses for this subscription, both in bodies and in the request path. */
    public String wireValue() {
        return wireValue;
    }

    /** This subscription as the core-owned identifier type. */
    public HookName hookName() {
        return HookName.of(wireValue);
    }

    /**
     * Resolve a wire value to its enum constant.
     *
     * @throws IllegalArgumentException if the API used a name this SDK version does not know
     */
    public static HookKind fromWireValue(String value) {
        for (HookKind kind : values()) {
            if (kind.wireValue.equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown hook name: " + value);
    }
}
