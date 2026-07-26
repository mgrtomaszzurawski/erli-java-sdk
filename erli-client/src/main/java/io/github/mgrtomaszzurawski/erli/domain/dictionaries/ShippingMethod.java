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
 * <p>{@link #maxDimensions()} is stated by the API in two different shapes and both occur in
 * practice, so it is a sealed {@link ParcelDimensions} — switch over {@code Box} and {@code Girth}
 * rather than assuming one. An empty {@code maxDimensions} means the API stated no bound.
 *
 * @param id the shipping-method identifier, e.g. {@code erliPaczkomat}
 * @param name the human-readable Polish label
 * @param groupId the delivery group this method belongs to, when stated
 * @param operator the logistics operator, when stated
 * @param cashOnDelivery whether the method supports cash on delivery (COD)
 * @param maxUnitPrice the API's {@code maxUnitPrice} verbatim. Deliberately NOT converted with
 *                     {@code Money.ofMinorUnits}, even though core now offers it: the spec gives this
 *                     field no unit ("Maksymalna cena dostawy") and this dictionary carries no
 *                     currency, while ERLI runs both a Polish and a German storefront. Building a
 *                     {@code Money} would mean hardcoding PLN. Convert it when the spec names a
 *                     currency, not before
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
