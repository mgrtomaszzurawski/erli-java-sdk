package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ParcelDimensions;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingOperator;
import io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethodMaxDimensions;
import io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethodMaxDimensionsAnyOf;
import io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethodMaxDimensionsAnyOf1;
import io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethodMaxPointDimensions;
import io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethodMinDimensions;

import java.util.Objects;
import java.util.Optional;

/**
 * Maps the generated Layer-1 shipping method to the public {@link ShippingMethod} domain record,
 * including the {@code anyOf} size bound, which the API states either as explicit box dimensions or
 * as a longest-side/dimensions-sum pair. Internal: never exported.
 */
final class ShippingMethodMapper {

    private ShippingMethodMapper() {
    }

    static ShippingMethod toDomain(io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod rawMethod) {
        Objects.requireNonNull(rawMethod, "raw ShippingMethod");
        return new ShippingMethod(
                ShippingMethodId.of(requireId(rawMethod)),
                requireName(rawMethod),
                Optional.ofNullable(rawMethod.getGroupId()).map(groupId -> groupId.getValue()),
                Optional.ofNullable(rawMethod.getOperator())
                        .map(operator -> ShippingOperator.fromWire(operator.getValue())),
                requireCashOnDelivery(rawMethod),
                Optional.ofNullable(rawMethod.getMaxUnitPrice()),
                Optional.ofNullable(rawMethod.getMinDimensions()).map(ShippingMethodMapper::toBox),
                Optional.ofNullable(rawMethod.getMaxDimensions()).flatMap(ShippingMethodMapper::toBound),
                Optional.ofNullable(rawMethod.getMaxPointDimensions()).map(ShippingMethodMapper::toBox));
    }

    private static String requireId(io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod rawMethod) {
        io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod.IdEnum id = rawMethod.getId();
        if (id == null) {
            throw new IllegalStateException("ShippingMethod is missing the required 'id' field");
        }
        return id.getValue();
    }

    private static String requireName(io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod rawMethod) {
        String name = rawMethod.getName();
        if (name == null) {
            throw new IllegalStateException("ShippingMethod is missing the required 'name' field");
        }
        return name;
    }

    private static boolean requireCashOnDelivery(
            io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod rawMethod) {
        Boolean cashOnDelivery = rawMethod.getCod();
        if (cashOnDelivery == null) {
            throw new IllegalStateException("ShippingMethod is missing the required 'cod' field");
        }
        return cashOnDelivery;
    }

    /**
     * The {@code anyOf} bound: box dimensions, or the longest-side/dimensions-sum form.
     *
     * <p><strong>Under-reports the girth form today (BACKLOG CORE-3).</strong> The generated
     * discriminator tries its branches in order and returns the first that binds, but core's
     * {@code JsonCodec} disables {@code FAIL_ON_UNKNOWN_PROPERTIES}, so the box branch accepts a girth
     * payload — every field unknown to it is ignored and every field it declares stays null. By the
     * time the mapper runs, {@code longestSide} and {@code dimensionsSum} are already gone, so no
     * mapping can recover them.
     *
     * <p>A box branch with no linear dimension at all is therefore a mis-bound girth payload rather
     * than a real bound, and is reported as absent instead of as a box whose dimensions are all
     * unknown — an absent bound is checked by callers, a zero-dimension box silently is not. Roughly
     * 20 of the sandbox's 49 shipping methods use the girth form, so this is not a rare edge case; it
     * is fixed for good in core, not here (see BACKLOG for the one-line mix-in fix).
     */
    private static Optional<ParcelDimensions> toBound(ShippingMethodMaxDimensions rawBound) {
        Object actual = rawBound.getActualInstance();
        if (actual instanceof ShippingMethodMaxDimensionsAnyOf box) {
            if (box.getHeight() == null && box.getWidth() == null && box.getLength() == null) {
                return Optional.empty();
            }
            return Optional.of(new ParcelDimensions.Box(
                    Optional.ofNullable(box.getHeight()),
                    Optional.ofNullable(box.getWidth()),
                    Optional.ofNullable(box.getLength()),
                    Optional.ofNullable(box.getWeight()),
                    Optional.ofNullable(box.getWithVolumetricScales())));
        }
        if (actual instanceof ShippingMethodMaxDimensionsAnyOf1 girth) {
            return Optional.of(new ParcelDimensions.Girth(
                    Optional.ofNullable(girth.getLongestSide()),
                    Optional.ofNullable(girth.getDimensionsSum()),
                    Optional.ofNullable(girth.getWeight()),
                    Optional.ofNullable(girth.getWithVolumetricScales())));
        }
        throw new IllegalStateException(
                "ShippingMethod.maxDimensions is neither of the two documented shapes, got: "
                        + (actual == null ? "null" : actual.getClass().getName()));
    }

    private static ParcelDimensions toBox(ShippingMethodMinDimensions rawBox) {
        return new ParcelDimensions.Box(
                Optional.ofNullable(rawBox.getHeight()),
                Optional.ofNullable(rawBox.getWidth()),
                Optional.ofNullable(rawBox.getLength()),
                Optional.ofNullable(rawBox.getWeight()),
                Optional.ofNullable(rawBox.getWithVolumetricScales()));
    }

    private static ParcelDimensions toBox(ShippingMethodMaxPointDimensions rawBox) {
        return new ParcelDimensions.Box(
                Optional.ofNullable(rawBox.getHeight()),
                Optional.ofNullable(rawBox.getWidth()),
                Optional.ofNullable(rawBox.getLength()),
                Optional.ofNullable(rawBox.getWeight()),
                Optional.ofNullable(rawBox.getWithVolumetricScales()));
    }
}
