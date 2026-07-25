package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void everyParcelStatusRoundTripsThroughItsWireValue() {
        for (ParcelStatus status : ParcelStatus.values()) {
            assertSame(status, ParcelStatus.fromWire(status.wireValue()), status.name());
        }
    }

    @Test
    void parcelStatusWireValuesAreDistinctAndNonBlank() {
        long distinct = Arrays.stream(ParcelStatus.values()).map(ParcelStatus::wireValue).distinct().count();

        assertEquals(ParcelStatus.values().length, distinct);
        assertTrue(Arrays.stream(ParcelStatus.values()).noneMatch(status -> status.wireValue().isBlank()));
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
     * The drift guard fires only if this domain enum falls out of sync with the generated one — an
     * unknown value from the live API fails earlier, at JSON decode. It must still name what went
     * wrong rather than return null and let a half-built record escape.
     */
    @Test
    void anUnmappedWireValueFailsLoudlyInsteadOfResolvingToNull() {
        ErliTransportException failure = assertThrows(
                ErliTransportException.class, () -> ParcelStatus.fromWire(UNKNOWN_WIRE_VALUE));

        assertTrue(failure.getMessage().contains(UNKNOWN_WIRE_VALUE), failure.getMessage());
        assertThrows(ErliTransportException.class, () -> ParcelType.fromWire(UNKNOWN_WIRE_VALUE));
        assertThrows(ErliTransportException.class, () -> PickupType.fromWire(UNKNOWN_WIRE_VALUE));
        assertThrows(ErliTransportException.class, () -> ShippingCountry.fromWire(UNKNOWN_WIRE_VALUE));
    }
}
