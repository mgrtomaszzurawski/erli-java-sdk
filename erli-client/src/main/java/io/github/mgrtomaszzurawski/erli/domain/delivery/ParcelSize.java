package io.github.mgrtomaszzurawski.erli.domain.delivery;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * InPost locker size bracket. Only {@code erliPaczkomat} prices per bracket; every other delivery
 * method takes a single item limit instead.
 */
public enum ParcelSize {

    /** Smallest locker compartment. */
    A("A"),
    /** Medium locker compartment. */
    B("B"),
    /** Largest locker compartment. */
    C("C");

    private static final Map<String, ParcelSize> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ParcelSize::wireValue, Function.identity()));

    private final String wireValue;

    ParcelSize(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this size is sent and received as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a size bracket.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value
     */
    public static ParcelSize fromWire(String wireValue) {
        ParcelSize size = BY_WIRE.get(wireValue);
        if (size == null) {
            throw new ErliTransportException("No ParcelSize constant maps wire value '" + wireValue + "'");
        }
        return size;
    }
}
