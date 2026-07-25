package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The carrier behind an externally shipped parcel — who the seller handed the package to when Erli's
 * own integration was not used.
 *
 * <p>Note this vocabulary is the same one the Dictionaries bucket exposes as its delivery-vendor
 * dictionary. It is duplicated here rather than imported because buckets must not depend on each
 * other; promoting one shared copy to {@code core.model} is tracked as CORE-7 in {@code BACKLOG.md}.
 */
public enum ShippingVendor {

    /** inpost. */
    INPOST("inpost"),
    /** pocztaPolska. */
    POCZTA_POLSKA("pocztaPolska"),
    /** dhl. */
    DHL("dhl"),
    /** dpd. */
    DPD("dpd"),
    /** dts. */
    DTS("dts"),
    /** fedex. */
    FEDEX("fedex"),
    /** pocztex24. */
    POCZTEX24("pocztex24"),
    /** rhenus. */
    RHENUS("rhenus"),
    /** raben. */
    RABEN("raben"),
    /** gls. */
    GLS("gls"),
    /** ups. */
    UPS("ups"),
    /** ruch. */
    RUCH("ruch"),
    /** orlen. */
    ORLEN("orlen"),
    /** geis. */
    GEIS("geis"),
    /** patronService. */
    PATRON_SERVICE("patronService"),
    /** pekaes. */
    PEKAES("pekaes"),
    /** tntExpress. */
    TNT_EXPRESS("tntExpress"),
    /** schenker. */
    SCHENKER("schenker"),
    /** ambroExpress. */
    AMBRO_EXPRESS("ambroExpress"),
    /** dsv. */
    DSV("dsv"),
    /** jasFBG. */
    JAS_FBG("jasFBG"),
    /** rohligSuus. */
    ROHLIG_SUUS("rohligSuus"),
    /** hellmann. */
    HELLMANN("hellmann"),
    /** ownTransport. */
    OWN_TRANSPORT("ownTransport"),
    /** selfPickup. */
    SELF_PICKUP("selfPickup"),
    /** other. */
    OTHER("other");

    private static final Map<String, ShippingVendor> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ShippingVendor::wireValue, Function.identity()));

    private final String wireValue;

    ShippingVendor(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this vendor is sent and received as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a vendor.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static ShippingVendor fromWire(String wireValue) {
        ShippingVendor vendor = BY_WIRE.get(wireValue);
        if (vendor == null) {
            throw new ErliTransportException("No ShippingVendor constant maps wire value '" + wireValue
                    + "'; this domain enum is out of sync with the generated model");
        }
        return vendor;
    }
}
