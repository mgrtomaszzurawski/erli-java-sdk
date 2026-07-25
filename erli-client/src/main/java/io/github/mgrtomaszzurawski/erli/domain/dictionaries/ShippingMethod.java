package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;

import java.util.Objects;
import java.util.Optional;

/**
 * An ERLI shipping method — a concrete, priced way to ship a parcel, as opposed to the broader
 * {@link DeliveryMethod} a buyer sees on an order.
 *
 * <p>{@code groupId} and {@code operator} are {@code Optional} although the spec marks them required:
 * since CORE-12 an operator or group the marketplace adds but the vendored spec lacks decodes to
 * {@code null}, and degrading one field is better than failing the whole dictionary.
 *
 * <p><strong>{@link #maxDimensions()} is currently under-reported.</strong> The API states this bound
 * in two shapes and the generated discriminator cannot tell them apart (BACKLOG CORE-3), so for the
 * longest-side/dimensions-sum form — about 20 of the sandbox's 49 methods — the bound is reported as
 * absent rather than wrong. Treat an empty {@code maxDimensions} as "unknown", not as "unbounded",
 * until that lands.
 *
 * @param id the shipping-method identifier, e.g. {@code erliPaczkomat}
 * @param name the human-readable Polish label
 * @param groupId the delivery group this method belongs to, when stated
 * @param operator the logistics operator, when stated
 * @param cashOnDelivery whether the method supports cash on delivery (COD)
 * @param maxUnitPrice the API's {@code maxUnitPrice} verbatim; the spec states no unit, so it is
 *                     carried through unconverted rather than guessed at as {@code Money}
 * @param minDimensions the lower dimension bound, when stated
 * @param maxDimensions the upper dimension bound, when stated
 * @param maxPointDimensions the upper dimension bound for pickup-point delivery, when stated
 */
public record ShippingMethod(
        ShippingMethodId id,
        String name,
        Optional<String> groupId,
        Optional<ShippingOperator> operator,
        boolean cashOnDelivery,
        Optional<Integer> maxUnitPrice,
        Optional<ParcelDimensions> minDimensions,
        Optional<ParcelDimensions> maxDimensions,
        Optional<ParcelDimensions> maxPointDimensions) {

    public ShippingMethod {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(groupId, "groupId");
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(maxUnitPrice, "maxUnitPrice");
        Objects.requireNonNull(minDimensions, "minDimensions");
        Objects.requireNonNull(maxDimensions, "maxDimensions");
        Objects.requireNonNull(maxPointDimensions, "maxPointDimensions");
    }
}
