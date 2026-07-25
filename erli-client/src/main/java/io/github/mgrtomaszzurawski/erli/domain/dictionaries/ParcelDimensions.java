package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * A size/weight bound on a shipping method. The API states such a bound in one of two ways, and both
 * occur in practice, so this is a sealed sum type rather than one record with half its fields null:
 *
 * <ul>
 *   <li>{@link Box} — explicit {@code height} × {@code width} × {@code length};</li>
 *   <li>{@link Girth} — a {@code longestSide} plus a {@code dimensionsSum}, the form couriers use for
 *       oversized parcels.</li>
 * </ul>
 *
 * <p><strong>The two forms use different length units</strong>, as the spec defines them:
 * {@link Box} edges are in <em>millimetres</em> (bounded 1–2000), while {@link Girth}'s
 * {@code longestSide} and {@code dimensionsSum} are in <em>centimetres</em> (bounded 1–200). Reading
 * a box edge as centimetres is out by a factor of ten. {@code weight} is in grams in both (the
 * sandbox reports {@code 25000} for a 25 kg method). Switch over the two cases to read a bound:
 *
 * <pre>{@code
 * String describe(ParcelDimensions bound) {
 *     return switch (bound) {
 *         case ParcelDimensions.Box box -> box.height() + "x" + box.width() + "x" + box.length();
 *         case ParcelDimensions.Girth girth -> "longest " + girth.longestSide();
 *     };
 * }
 * }</pre>
 */
public sealed interface ParcelDimensions permits ParcelDimensions.Box, ParcelDimensions.Girth {

    /** The weight bound in grams, when stated. */
    Optional<Integer> weight();

    /** Whether the bound applies with volumetric scaling, when stated. */
    Optional<Boolean> withVolumetricScales();

    /**
     * A bound given as explicit box dimensions.
     *
     * @param height the height bound in millimetres, when stated
     * @param width the width bound in millimetres, when stated
     * @param length the length bound in millimetres, when stated
     * @param weight the weight bound in grams, when stated
     * @param withVolumetricScales whether the bound applies with volumetric scaling, when stated
     */
    record Box(
            Optional<BigDecimal> height,
            Optional<BigDecimal> width,
            Optional<BigDecimal> length,
            Optional<Integer> weight,
            Optional<Boolean> withVolumetricScales) implements ParcelDimensions {

        public Box {
            Objects.requireNonNull(height, "height");
            Objects.requireNonNull(width, "width");
            Objects.requireNonNull(length, "length");
            Objects.requireNonNull(weight, "weight");
            Objects.requireNonNull(withVolumetricScales, "withVolumetricScales");
        }
    }

    /**
     * A bound given as a longest side plus a sum of dimensions, the form used for oversized parcels.
     *
     * @param longestSide the longest permitted side in centimetres, when stated
     * @param dimensionsSum the permitted sum of dimensions in centimetres, when stated
     * @param weight the weight bound in grams, when stated
     * @param withVolumetricScales whether the bound applies with volumetric scaling, when stated
     */
    record Girth(
            Optional<BigDecimal> longestSide,
            Optional<BigDecimal> dimensionsSum,
            Optional<Integer> weight,
            Optional<Boolean> withVolumetricScales) implements ParcelDimensions {

        public Girth {
            Objects.requireNonNull(longestSide, "longestSide");
            Objects.requireNonNull(dimensionsSum, "dimensionsSum");
            Objects.requireNonNull(weight, "weight");
            Objects.requireNonNull(withVolumetricScales, "withVolumetricScales");
        }
    }
}
