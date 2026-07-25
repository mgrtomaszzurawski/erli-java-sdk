package io.github.mgrtomaszzurawski.erli.domain.delivery;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Unit in which a delivery method's promised delivery window is expressed. */
public enum DeliveryTimeUnit {

    /** The window is stated in days. */
    DAYS("days"),
    /** The window is stated in hours. */
    HOURS("hours");

    private static final Map<String, DeliveryTimeUnit> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(DeliveryTimeUnit::wireValue, Function.identity()));

    private final String wireValue;

    DeliveryTimeUnit(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this unit is sent and received as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a unit.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static DeliveryTimeUnit fromWire(String wireValue) {
        DeliveryTimeUnit unit = BY_WIRE.get(wireValue);
        if (unit == null) {
            throw new ErliTransportException("No DeliveryTimeUnit constant maps wire value '" + wireValue
                    + "'; this domain enum is out of sync with the generated model");
        }
        return unit;
    }
}
