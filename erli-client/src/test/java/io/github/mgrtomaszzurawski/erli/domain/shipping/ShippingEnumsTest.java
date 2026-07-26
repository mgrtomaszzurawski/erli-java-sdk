package io.github.mgrtomaszzurawski.erli.domain.shipping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the wire mapping of the shipping enums, mirroring what {@code DeliveryVendorTest} does for the
 * Dictionaries bucket.
 *
 * <p>These enums are the SDK's copy of vocabularies the spec repeats inline in five places. The risk
 * they carry is silent drift: a constant renamed or a wire string mistyped would not fail to compile,
 * it would fail at runtime on a live payload. A round trip through {@link ParcelStatus#wireValue()} and
 * {@code fromWire} catches that, and the write endpoints this bucket is about to add depend on the
 * reverse direction being exact.
 */
class ShippingEnumsTest {

    private static final String UNKNOWN_WIRE_VALUE = "somethingErliAddedLater";

    @Test
    void everyKnownParcelStatusRoundTripsThroughItsWireValue() {
        for (ParcelStatus status : ParcelStatus.values()) {
            if (status == ParcelStatus.UNRECOGNIZED) {
                continue;
            }
            assertSame(status, ParcelStatus.fromWire(status.wireValue()), status.name());
        }
    }

    @Test
    void knownParcelStatusWireValuesAreDistinctAndNonBlank() {
        List<ParcelStatus> known = Arrays.stream(ParcelStatus.values())
                .filter(status -> status != ParcelStatus.UNRECOGNIZED)
                .toList();

        assertEquals(known.size(), known.stream().map(ParcelStatus::wireValue).distinct().count());
        assertTrue(known.stream().noneMatch(status -> status.wireValue().isBlank()));
    }

    @Test
    void everySmallShippingEnumRoundTripsThroughItsWireValue() {
        for (ParcelType type : ParcelType.values()) {
            assertSame(type, ParcelType.fromWire(type.wireValue()), type.name());
        }
        for (PickupType pickupType : PickupType.values()) {
            assertSame(pickupType, PickupType.fromWire(pickupType.wireValue()), pickupType.name());
        }
        for (ShippingCountry country : ShippingCountry.values()) {
            assertSame(country, ShippingCountry.fromWire(country.wireValue()), country.name());
        }
    }

    /**
     * Parcel status is an open, carrier-driven vocabulary: a value Erli mints after this release must
     * degrade to a sentinel, because failing here would cost a caller every other parcel in the same
     * search response.
     */
    @Test
    void anUnknownParcelStatusDegradesToTheSentinelRatherThanFailingTheRead() {
        assertSame(ParcelStatus.UNRECOGNIZED, ParcelStatus.fromWire(UNKNOWN_WIRE_VALUE));
        assertSame(ParcelStatus.UNRECOGNIZED, ParcelStatus.fromWire(null));
    }

    /**
     * The closed vocabularies keep the opposite contract on purpose. For these an unknown value means
     * something is genuinely wrong — a country the SDK cannot address, a pickup type it cannot honour —
     * so it must name what happened rather than let a half-built record escape.
     */
    @Test
    void anUnmappedValueOfAClosedEnumStillFailsLoudly() {
        ErliTransportException failure = assertThrows(
                ErliTransportException.class, () -> ParcelType.fromWire(UNKNOWN_WIRE_VALUE));

        assertTrue(failure.getMessage().contains(UNKNOWN_WIRE_VALUE), failure.getMessage());
        assertThrows(ErliTransportException.class, () -> PickupType.fromWire(UNKNOWN_WIRE_VALUE));
        assertThrows(ErliTransportException.class, () -> ShippingCountry.fromWire(UNKNOWN_WIRE_VALUE));
        assertThrows(ErliTransportException.class, () -> PostingPointType.fromWire(UNKNOWN_WIRE_VALUE));
    }
}
