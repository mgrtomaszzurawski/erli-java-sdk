package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.Objects;

/**
 * A carrier (shipping company) a delivery method is operated by, e.g. {@code inpost} or {@code dhl}.
 *
 * <p>Deliberately a value type rather than a Java {@code enum}: the carrier list is server-provided
 * reference data — {@code GET /dictionaries/deliveryVendors} exists precisely because it can grow —
 * so a marketplace adding a carrier must not break a compiled consumer. Compare against the constants
 * below, or against {@link #value()} for a carrier this SDK version does not yet name.
 *
 * @param value the non-blank carrier identifier exactly as the API spells it
 */
public record DeliveryVendor(String value) implements Comparable<DeliveryVendor> {

    public static final DeliveryVendor INPOST = new DeliveryVendor("inpost");
    public static final DeliveryVendor POCZTA_POLSKA = new DeliveryVendor("pocztaPolska");
    public static final DeliveryVendor DHL = new DeliveryVendor("dhl");
    public static final DeliveryVendor DPD = new DeliveryVendor("dpd");
    public static final DeliveryVendor GLS = new DeliveryVendor("gls");
    public static final DeliveryVendor UPS = new DeliveryVendor("ups");
    public static final DeliveryVendor ORLEN = new DeliveryVendor("orlen");
    public static final DeliveryVendor OWN_TRANSPORT = new DeliveryVendor("ownTransport");
    public static final DeliveryVendor SELF_PICKUP = new DeliveryVendor("selfPickup");
    public static final DeliveryVendor OTHER = new DeliveryVendor("other");

    public DeliveryVendor {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DeliveryVendor must not be null or blank");
        }
        value = value.trim();
    }

    public static DeliveryVendor of(String value) {
        return new DeliveryVendor(value);
    }

    @Override
    public int compareTo(DeliveryVendor other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }

    @Override
    public String toString() {
        return value;
    }
}
