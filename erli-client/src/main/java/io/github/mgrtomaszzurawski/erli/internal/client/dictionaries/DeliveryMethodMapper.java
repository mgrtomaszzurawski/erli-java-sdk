package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;

import java.util.Objects;

/**
 * Maps the generated Layer-1 {@code DeliveryMethod} to the public domain {@link DeliveryMethod}. The
 * vendor is resolved by its wire value (not the generated enum name). Kept internal so the {@code *Raw}
 * type never appears in an exported signature. Internal.
 */
final class DeliveryMethodMapper {

    private DeliveryMethodMapper() {
    }

    static DeliveryMethod toDomain(io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod rawMethod) {
        Objects.requireNonNull(rawMethod, "raw DeliveryMethod");
        return new DeliveryMethod(
                DeliveryMethodId.of(requireField(rawMethod.getId(), "id")),
                requireField(rawMethod.getName(), "name"),
                requireCashOnDelivery(rawMethod),
                requireVendor(rawMethod));
    }

    private static String requireField(String value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("DeliveryMethod is missing the required '" + fieldName + "' field");
        }
        return value;
    }

    private static boolean requireCashOnDelivery(io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod rawMethod) {
        Boolean cod = rawMethod.getCod();
        if (cod == null) {
            throw new IllegalStateException("DeliveryMethod is missing the required 'cod' field");
        }
        return cod;
    }

    private static DeliveryVendor requireVendor(io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod rawMethod) {
        io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod.VendorEnum vendor = rawMethod.getVendor();
        if (vendor == null) {
            throw new IllegalStateException("DeliveryMethod is missing the required 'vendor' field");
        }
        return DeliveryVendor.fromWire(vendor.getValue());
    }
}
