package io.github.mgrtomaszzurawski.erli.core.model;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A marketplace storefront — ERLI runs a Polish and a German one. Mapped by wire string via
 * {@link #fromWire}, like every other wire-carrying enum, so each wire value is written exactly once.
 */
public enum Market {

    /** The Polish storefront, {@code pl}. */
    POLAND("pl"),

    /** The German storefront, {@code de}. */
    GERMANY("de");

    private static final Map<String, Market> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(Market::wireValue, Function.identity()));

    private final String wireValue;

    Market(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this market is sent as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a market.
     *
     * <p>Since CORE-12 the codec decodes an unrecognised wire value to {@code null} rather than
     * throwing, so this method never sees one: the caller maps the {@code null} itself. It therefore
     * fires only if this domain enum drifts out of sync with the generated one.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static Market fromWire(String wireValue) {
        Market market = BY_WIRE.get(wireValue);
        if (market == null) {
            throw new ErliTransportException(
                    "No Market constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return market;
    }
}
