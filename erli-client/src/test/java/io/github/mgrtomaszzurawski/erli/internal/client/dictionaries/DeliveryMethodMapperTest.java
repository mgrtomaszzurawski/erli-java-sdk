package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod.VendorEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryMethodMapperTest {

    private static io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod raw() {
        return new io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod()
                .id("courier-1")
                .name("Kurier")
                .cod(true)
                .vendor(VendorEnum.INPOST);
    }

    @Test
    void mapsAllFieldsAndResolvesVendorByWireValue() {
        DeliveryMethod method = DeliveryMethodMapper.toDomain(raw());

        assertEquals("courier-1", method.id().value());
        assertEquals("Kurier", method.name());
        assertTrue(method.cashOnDelivery());
        assertEquals(DeliveryVendor.INPOST, method.vendor());
    }

    @Test
    void mapsCashOnDeliveryFalse() {
        DeliveryMethod method = DeliveryMethodMapper.toDomain(raw().cod(false));
        assertFalse(method.cashOnDelivery());
    }

    @Test
    void rejectsMissingRequiredVendor() {
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod raw = raw().vendor(null);
        assertThrows(IllegalStateException.class, () -> DeliveryMethodMapper.toDomain(raw));
    }

    @Test
    void rejectsMissingRequiredCod() {
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod raw = raw().cod(null);
        assertThrows(IllegalStateException.class, () -> DeliveryMethodMapper.toDomain(raw));
    }

    @Test
    void rejectsMissingRequiredId() {
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod raw = raw().id(null);
        assertThrows(IllegalStateException.class, () -> DeliveryMethodMapper.toDomain(raw));
    }

    @Test
    void rejectsMissingRequiredName() {
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod raw = raw().name(null);
        assertThrows(IllegalStateException.class, () -> DeliveryMethodMapper.toDomain(raw));
    }
}
