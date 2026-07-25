package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A carrier (vendor) a delivery method belongs to. Values mirror the Erli dictionary; the SDK maps by
 * the wire string (via {@link #fromWire}), not by enum name, so the domain constant names stay
 * decoupled from the generated Layer-1 ones.
 */
public enum DeliveryVendor {

    INPOST("inpost"),
    POCZTA_POLSKA("pocztaPolska"),
    DHL("dhl"),
    DPD("dpd"),
    DTS("dts"),
    FEDEX("fedex"),
    POCZTEX24("pocztex24"),
    RHENUS("rhenus"),
    RABEN("raben"),
    GLS("gls"),
    UPS("ups"),
    RUCH("ruch"),
    ORLEN("orlen"),
    GEIS("geis"),
    PATRON_SERVICE("patronService"),
    PEKAES("pekaes"),
    TNT_EXPRESS("tntExpress"),
    SCHENKER("schenker"),
    AMBRO_EXPRESS("ambroExpress"),
    DSV("dsv"),
    JAS_FBG("jasFBG"),
    ROHLIG_SUUS("rohligSuus"),
    HELLMANN("hellmann"),
    OWN_TRANSPORT("ownTransport"),
    SELF_PICKUP("selfPickup"),
    OTHER("other");

    private static final Map<String, DeliveryVendor> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(DeliveryVendor::wireValue, Function.identity()));

    private final String wireValue;

    DeliveryVendor(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this vendor is sent as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a vendor.
     *
     * <p>Since CORE-12 the codec decodes an unknown wire value to a {@code null} Layer-1
     * {@code VendorEnum} (it no longer throws at decode), and the mapper rejects that null before
     * calling this method — so {@code fromWire} only ever sees the value of a known {@code VendorEnum}.
     * This guard therefore fires only if this domain enum drifts out of sync with the generated one.
     * (A future move to tolerant handling would add an {@code UNRECOGNIZED} constant and map null to
     * it in the mapper; see {@code KNOWN-SERVER-BEHAVIORS.md} "Enum handling".)
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static DeliveryVendor fromWire(String wireValue) {
        DeliveryVendor vendor = BY_WIRE.get(wireValue);
        if (vendor == null) {
            throw new ErliTransportException(
                    "No DeliveryVendor constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return vendor;
    }
}
