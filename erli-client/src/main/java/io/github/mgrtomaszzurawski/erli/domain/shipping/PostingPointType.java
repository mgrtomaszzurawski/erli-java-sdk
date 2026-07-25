package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** How a posting point hands parcels over to the carrier. */
public enum PostingPointType {

    /** The courier collects from the seller's address. */
    ADDRESS("address"),
    /** The seller drops parcels at one carrier point. */
    POINT("point"),
    /** The seller drops parcels at any of several carrier points. */
    POINTS("points");

    private static final Map<String, PostingPointType> BY_WIRE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(PostingPointType::wireValue, Function.identity()));

    private final String wireValue;

    PostingPointType(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact string this type is sent and received as on the wire. */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolve a wire string to a posting-point type.
     *
     * @throws ErliTransportException if no domain constant maps the given wire value (enum drift)
     */
    public static PostingPointType fromWire(String wireValue) {
        PostingPointType type = BY_WIRE.get(wireValue);
        if (type == null) {
            throw new ErliTransportException("No PostingPointType constant maps wire value '" + wireValue + "'");
        }
        return type;
    }
}
