package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Who carries out the shipment. Erli models the two kinds as separate resources with a single-valued
 * {@code type} discriminator each; the SDK exposes one enum so a caller can branch on it uniformly.
 * Mapped by wire string like the other shipping enums.
 */
public enum ParcelType {

    /** Shipped through Erli's own carrier integration ({@code /shipping/parcels}). */
    INTERNAL("internal"),
    /** Shipped by the seller outside Erli, tracked here only ({@code /shipping/external}). */
    EXTERNAL("external");

    private static final Map<String, ParcelType> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ParcelType::wireValue, Function.identity()));

    private final String wireValue;

    ParcelType(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this type is sent and received as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a parcel type.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static ParcelType fromWire(String wireValue) {
        ParcelType type = BY_WIRE.get(wireValue);
        if (type == null) {
            throw new ErliTransportException(
                    "No ParcelType constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return type;
    }
}
