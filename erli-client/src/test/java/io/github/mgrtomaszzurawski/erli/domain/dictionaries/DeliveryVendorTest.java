package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

class DeliveryVendorTest {

    @Test
    void resolvesKnownWireValues() {
        assertEquals(DeliveryVendor.INPOST, DeliveryVendor.fromWire("inpost"));
        assertEquals(DeliveryVendor.POCZTA_POLSKA, DeliveryVendor.fromWire("pocztaPolska"));
        assertEquals(DeliveryVendor.OTHER, DeliveryVendor.fromWire("other"));
    }

    @Test
    void everyConstantRoundTripsThroughItsWireValue() {
        for (DeliveryVendor vendor : DeliveryVendor.values()) {
            assertSame(vendor, DeliveryVendor.fromWire(vendor.wireValue()));
        }
    }

    @Test
    void rejectsUnknownWireValueLoudly() {
        assertThrows(ErliTransportException.class, () -> DeliveryVendor.fromWire("teleportation"));
    }
}
