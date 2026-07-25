package io.github.mgrtomaszzurawski.erli.domain.orders;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The carrier a parcel was handed to, as reported on {@link DeliveryTracking}.
 *
 * <p>Deliberately <strong>not</strong> a Java {@code enum}. The carrier list is a growing reference
 * set, not a closed domain: the spec enumerates 26 values, the {@code deliveryVendors} dictionary
 * returns 10, and {@code DeliveryMethod.vendor} has been observed spanning 23 — the three do not
 * agree, and the list gains entries as Erli signs carriers (see {@code KNOWN-SERVER-BEHAVIORS.md}).
 * A closed enum would force a release of this SDK before a consumer could see a new carrier, and
 * would turn an unrecognised value into an exception rather than data.
 *
 * <p>So a vendor is a value object over its wire name. The constants below are the values the spec
 * lists today and are safe to compare with {@link #equals}; anything else Erli sends round-trips
 * intact through {@link #of(String)} and is simply not one of them.
 *
 * <pre>{@code
 * if (ShippingVendor.INPOST.equals(tracking.vendor().orElse(null))) { ... }
 * String forMyErp = vendor.wireValue();
 * }</pre>
 */
public final class ShippingVendor {

    private static final Map<String, ShippingVendor> KNOWN = new LinkedHashMap<>();

    public static final ShippingVendor INPOST = known("inpost");
    public static final ShippingVendor POCZTA_POLSKA = known("pocztaPolska");
    public static final ShippingVendor POCZTEX_24 = known("pocztex24");
    public static final ShippingVendor DHL = known("dhl");
    public static final ShippingVendor DPD = known("dpd");
    public static final ShippingVendor DTS = known("dts");
    public static final ShippingVendor FEDEX = known("fedex");
    public static final ShippingVendor RHENUS = known("rhenus");
    public static final ShippingVendor RABEN = known("raben");
    public static final ShippingVendor GLS = known("gls");
    public static final ShippingVendor UPS = known("ups");
    public static final ShippingVendor RUCH = known("ruch");
    public static final ShippingVendor ORLEN = known("orlen");
    public static final ShippingVendor GEIS = known("geis");
    public static final ShippingVendor PATRON_SERVICE = known("patronService");
    public static final ShippingVendor PEKAES = known("pekaes");
    public static final ShippingVendor TNT_EXPRESS = known("tntExpress");
    public static final ShippingVendor SCHENKER = known("schenker");
    public static final ShippingVendor AMBRO_EXPRESS = known("ambroExpress");
    public static final ShippingVendor DSV = known("dsv");
    public static final ShippingVendor JAS_FBG = known("jasFBG");
    public static final ShippingVendor ROHLIG_SUUS = known("rohligSuus");
    public static final ShippingVendor HELLMANN = known("hellmann");

    /** Delivered by the seller's own transport. */
    public static final ShippingVendor OWN_TRANSPORT = known("ownTransport");

    /** Collected in person by the buyer. */
    public static final ShippingVendor SELF_PICKUP = known("selfPickup");

    /** Erli's own catch-all — distinct from a carrier this SDK simply does not know yet. */
    public static final ShippingVendor OTHER = known("other");

    private final String wireValue;

    private ShippingVendor(String wireValue) {
        this.wireValue = wireValue;
    }

    private static ShippingVendor known(String wireValue) {
        ShippingVendor vendor = new ShippingVendor(wireValue);
        KNOWN.put(wireValue, vendor);
        return vendor;
    }

    /**
     * The vendor for a wire value: one of the constants above when Erli sends a value this SDK knows,
     * otherwise a new instance carrying that value verbatim.
     *
     * @param wireValue the carrier name exactly as it appears on the wire
     * @return the matching vendor, never {@code null}
     * @throws IllegalArgumentException if {@code wireValue} is blank
     */
    public static ShippingVendor of(String wireValue) {
        Objects.requireNonNull(wireValue, "wireValue");
        if (wireValue.isBlank()) {
            throw new IllegalArgumentException("A shipping vendor's wire value must not be blank");
        }
        ShippingVendor recognized = KNOWN.get(wireValue);
        return recognized != null ? recognized : new ShippingVendor(wireValue);
    }

    /** The carrier name as Erli spells it — what to hand on to an ERP or a carrier integration. */
    public String wireValue() {
        return wireValue;
    }

    /** Whether this is one of the carriers the spec listed when this SDK was built. */
    public boolean isKnown() {
        return KNOWN.containsKey(wireValue);
    }

    /** Every carrier the spec listed when this SDK was built, in spec order. */
    public static Collection<ShippingVendor> known() {
        return List.copyOf(KNOWN.values());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ShippingVendor that && wireValue.equals(that.wireValue);
    }

    @Override
    public int hashCode() {
        return wireValue.hashCode();
    }

    @Override
    public String toString() {
        return wireValue;
    }
}
