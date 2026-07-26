package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * How the buyer receives the parcel: at their address, or at a carrier pickup point. Mapped by wire
 * string like the other shipping enums, so the same constants serve reads and writes.
 */
public enum PickupType {

    /** Door-to-door courier delivery. */
    COURIER("courier"),
    /** Collection at a pickup point identified by {@link ShippingParty#pointCode()}. */
    POINT("point");

    private static final Map<String, PickupType> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(PickupType::wireValue, Function.identity()));

    private final String wireValue;

    PickupType(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this pickup type is sent and received as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a pickup type.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static PickupType fromWire(String wireValue) {
        PickupType pickupType = BY_WIRE.get(wireValue);
        if (pickupType == null) {
            throw new ErliTransportException(
                    "No PickupType constant maps wire value '" + wireValue
                            + "'; this domain enum is out of sync with the generated model");
        }
        return pickupType;
    }
}
