package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.mgrtomaszzurawski.erli.core.model.ShippingMethodId;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ParcelDimensions;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingMethod;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.ShippingOperator;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
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

    private static final String MAX_DIMENSIONS_FIELD = "maxDimensions";
    private static final String LONGEST_SIDE_FIELD = "longestSide";

    private ShippingMethodMapper() {
    }

    /**
     * Map one shipping method from its raw JSON.
     *
     * <p>Every field except the size bound comes straight from Layer 1 — the node is bound to the
     * generated model by the shared codec. Only {@code maxDimensions} needs the tree, because its
     * {@code anyOf} cannot be discriminated after decoding (see {@link #toBound}).
     */
    static ShippingMethod toDomain(JsonNode rawNode, JsonCodec codec) {
        Objects.requireNonNull(rawNode, "raw shipping method");
        Objects.requireNonNull(codec, "codec");
        io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod rawMethod =
                codec.convert(rawNode, io.github.mgrtomaszzurawski.erli.rest.model.ShippingMethod.class);
        return new ShippingMethod(
                ShippingMethodId.of(requireId(rawMethod)),
                requireName(rawMethod),
                Optional.ofNullable(rawMethod.getGroupId()).map(groupId -> groupId.getValue()),
                Optional.ofNullable(rawMethod.getOperator())
                        .map(operator -> ShippingOperator.fromWire(operator.getValue())),
                requireCashOnDelivery(rawMethod),
                Optional.ofNullable(rawMethod.getMaxUnitPrice()),
                Optional.ofNullable(rawMethod.getMinDimensions()).map(ShippingMethodMapper::toBox),
                toBound(rawNode.path(MAX_DIMENSIONS_FIELD), codec),
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
     * The {@code anyOf} size bound: explicit box dimensions, or the longest-side/dimensions-sum form.
     *
     * <p><strong>CORE-3 workaround — tree dispatch, not hand-copied fields.</strong> The generated
     * {@code anyOf} deserializer takes the first branch that parses, and because the shared codec
     * ignores unknown properties the box branch always parses: a girth payload would bind to it with
     * every declared field null, silently losing {@code longestSide} and {@code dimensionsSum}. About
     * 20 of the sandbox's 49 methods use that form. So the discriminator is read off the raw tree
     * ({@code longestSide} appears only on the girth branch) and the node is then bound to the right
     * generated type by the codec — every field still comes from Layer 1.
     *
     * <p>Unlike the tracking-shaped composites, this one is <em>not</em> fixed by CORE-3's
     * {@code normalizeSpec} merge, and correctly so: that rule refuses a composite whose branches
     * declare a property differently, and {@code weight} is {@code maximum: 700000} on the box branch
     * against {@code 50000} on the girth branch. Merging would document one bound for both. This
     * collapses to a single {@code getActualInstance()} switch only once core discriminates the
     * branches itself (the strict-branch mix-in in the BACKLOG).
     */
    private static Optional<ParcelDimensions> toBound(JsonNode rawBound, JsonCodec codec) {
        if (rawBound == null || !rawBound.isObject()) {
            return Optional.empty();
        }
        if (rawBound.hasNonNull(LONGEST_SIDE_FIELD)) {
            ShippingMethodMaxDimensionsAnyOf1 girth =
                    codec.convert(rawBound, ShippingMethodMaxDimensionsAnyOf1.class);
            return Optional.of(new ParcelDimensions.Girth(
                    Optional.ofNullable(girth.getLongestSide()),
                    Optional.ofNullable(girth.getDimensionsSum()),
                    Optional.ofNullable(girth.getWeight()),
                    Optional.ofNullable(girth.getWithVolumetricScales())));
        }
        ShippingMethodMaxDimensionsAnyOf box = codec.convert(rawBound, ShippingMethodMaxDimensionsAnyOf.class);
        return Optional.of(new ParcelDimensions.Box(
                Optional.ofNullable(box.getHeight()),
                Optional.ofNullable(box.getWidth()),
                Optional.ofNullable(box.getLength()),
                Optional.ofNullable(box.getWeight()),
                Optional.ofNullable(box.getWithVolumetricScales())));
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
