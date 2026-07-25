package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryVendor;
import io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod.VendorEnum;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Maps the generated Layer-1 delivery method to the public {@link DeliveryMethod} domain record. The
 * raw type shares its simple name with the domain record, so it stays fully qualified here; that
 * asymmetry is deliberate — it makes every crossing of the Layer-1 boundary visible. Kept in an
 * internal package so the raw type never appears in an exported signature. Internal.
 */
final class DeliveryMethodMapper {

    private DeliveryMethodMapper() {
    }

    static List<DeliveryMethod> toDomainList(io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod[] rawMethods) {
        if (rawMethods == null) {
            return List.of();
        }
        return Arrays.stream(rawMethods).map(DeliveryMethodMapper::toDomain).toList();
    }

    static DeliveryMethod toDomain(io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod rawMethod) {
        Objects.requireNonNull(rawMethod, "raw DeliveryMethod");
        return new DeliveryMethod(
                DeliveryMethodId.of(require(rawMethod.getId(), "id")),
                require(rawMethod.getName(), "name"),
                requireCashOnDelivery(rawMethod),
                toVendor(rawMethod));
    }

    private static String require(String value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("DeliveryMethod is missing the required '" + fieldName + "' field");
        }
        return value;
    }

    private static boolean requireCashOnDelivery(
            io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod rawMethod) {
        Boolean cod = rawMethod.getCod();
        if (cod == null) {
            throw new IllegalStateException("DeliveryMethod is missing the required 'cod' field");
        }
        return cod;
    }

    private static DeliveryVendor toVendor(io.github.mgrtomaszzurawski.erli.rest.model.DeliveryMethod rawMethod) {
        VendorEnum vendor = rawMethod.getVendor();
        if (vendor == null) {
            throw new IllegalStateException("DeliveryMethod is missing the required 'vendor' field");
        }
        // Carried through by value, not switched onto a domain enum: the carrier list is server-owned
        // reference data, so an unrecognised carrier must reach the caller rather than fail the mapping.
        return DeliveryVendor.of(vendor.getValue());
    }
}
