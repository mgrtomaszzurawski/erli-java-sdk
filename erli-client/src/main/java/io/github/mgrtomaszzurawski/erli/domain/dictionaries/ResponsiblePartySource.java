package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Where a {@link ResponsibleParty} entry came from — the API itself, the shop panel, or one of the
 * integrations ERLI supports. Mapped by wire string via {@link #fromWire}, per the fleet enum
 * guideline: the list names third-party integrations and grows as ERLI adds them.
 */
public enum ResponsiblePartySource {

    API("api"),
    MANUAL("manual"),
    ALLEGRO("allegro"),
    IDOSELL("idosell"),
    PRESTA_SHOP("prestaShop"),
    SHOPER("shoper"),
    BASELINKER("baselinker");

    private static final Map<String, ResponsiblePartySource> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ResponsiblePartySource::wireValue, Function.identity()));

    private final String wireValue;

    ResponsiblePartySource(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this source is sent as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a source.
     *
     * <p>Since CORE-12 the codec decodes an unrecognised wire value to {@code null} rather than
     * throwing, so this method never sees one: the caller maps the {@code null} itself. It therefore
     * fires only if this domain enum drifts out of sync with the generated one.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static ResponsiblePartySource fromWire(String wireValue) {
        ResponsiblePartySource source = BY_WIRE.get(wireValue);
        if (source == null) {
            throw new ErliTransportException(
                    "No ResponsiblePartySource constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return source;
    }
}
