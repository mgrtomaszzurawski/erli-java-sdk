package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

/**
 * The logistics operator behind an ERLI shipping method, e.g. {@code INPOST} or {@code DHL}.
 *
 * <p>A value type rather than a Java enum, for the same reason as {@link DeliveryVendor}: the
 * operator list is server-owned reference data and may grow without an SDK release.
 *
 * @param value the non-blank operator identifier exactly as the API spells it
 */
public record ShippingOperator(String value) {

    public static final ShippingOperator INPOST = new ShippingOperator("INPOST");
    public static final ShippingOperator DHL = new ShippingOperator("DHL");
    public static final ShippingOperator DPD = new ShippingOperator("DPD");
    public static final ShippingOperator POCZTA = new ShippingOperator("POCZTA");
    public static final ShippingOperator RUCH = new ShippingOperator("RUCH");

    public ShippingOperator {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ShippingOperator must not be null or blank");
        }
        value = value.trim();
    }

    public static ShippingOperator of(String value) {
        return new ShippingOperator(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
