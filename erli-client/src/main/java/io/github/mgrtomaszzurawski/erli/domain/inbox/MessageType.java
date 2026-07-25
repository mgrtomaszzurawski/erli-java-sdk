package io.github.mgrtomaszzurawski.erli.domain.inbox;

import java.util.Optional;

/**
 * What an inbox message announces. The API types this field as a free-form string rather than an
 * enum, so an unrecognised value decodes to {@link #UNKNOWN} instead of failing — the raw value stays
 * available via {@link Message#typeName()}.
 *
 * <p>{@link #ORDER_SELLER_STATUS_CHANGED} messages exist (the payload schema documents them) but the
 * search filter's own enum omits that value, so {@link #filterable()} is false for it — see
 * {@code KNOWN-SERVER-BEHAVIORS.md}.
 */
public enum MessageType {

    /** A new order was placed; the payload is the order snapshot. */
    ORDER_CREATED("orderCreated", true),
    /** The marketplace-side status of an order changed; the payload is the order snapshot. */
    ORDER_STATUS_CHANGED("orderStatusChanged", true),
    /** The seller-side status of an order changed; the payload is the order snapshot. */
    ORDER_SELLER_STATUS_CHANGED("orderSellerStatusChanged", false),
    /** Products need re-synchronising; the payload lists them. */
    PRODUCTS_NEED_SYNC("productsNeedSync", true),
    /** A type this SDK version does not know. */
    UNKNOWN("", false);

    private final String wireValue;
    private final boolean filterable;

    MessageType(String wireValue, boolean filterable) {
        this.wireValue = wireValue;
        this.filterable = filterable;
    }

    /** The value the API uses for this type. Empty for {@link #UNKNOWN}. */
    public String wireValue() {
        return wireValue;
    }

    /** Whether {@code POST /inbox/_search} accepts this type as a filter value. */
    public boolean filterable() {
        return filterable;
    }

    /** Whether a message of this type carries an {@link OrderEvent} payload. */
    public boolean carriesOrderEvent() {
        return this == ORDER_CREATED || this == ORDER_STATUS_CHANGED || this == ORDER_SELLER_STATUS_CHANGED;
    }

    /** Resolve a wire value, or empty when this SDK version does not know it. */
    public static Optional<MessageType> fromWireValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (MessageType type : values()) {
            if (type != UNKNOWN && type.wireValue.equals(value)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
