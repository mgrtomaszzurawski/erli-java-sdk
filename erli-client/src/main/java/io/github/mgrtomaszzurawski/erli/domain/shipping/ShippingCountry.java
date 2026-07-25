package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Destination country supported by Erli shipping. The API defaults an omitted country to {@link #PL}
 * for domestic parcels. Mapped by wire string like the other shipping enums, so the same constants
 * serve reads and writes.
 */
public enum ShippingCountry {

    /** Poland. */
    PL("pl"),
    /** Germany. */
    DE("de");

    private static final Map<String, ShippingCountry> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ShippingCountry::wireValue, Function.identity()));

    private final String wireValue;

    ShippingCountry(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this country is sent and received as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a country.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static ShippingCountry fromWire(String wireValue) {
        ShippingCountry country = BY_WIRE.get(wireValue);
        if (country == null) {
            throw new ErliTransportException(
                    "No ShippingCountry constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return country;
    }
}
