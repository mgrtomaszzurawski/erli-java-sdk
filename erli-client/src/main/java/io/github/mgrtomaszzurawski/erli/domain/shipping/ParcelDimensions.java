package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Physical size and weight of a parcel, as the carrier prices it.
 *
 * <p>Erli expresses the three edges in centimetres and the weight in grams. {@link BigDecimal} keeps
 * the edges exactly as the API states them — carrier size brackets are compared against these values,
 * so binary rounding must not creep in.
 *
 * @param width  width in centimetres
 * @param height height in centimetres
 * @param length length in centimetres
 * @param weight weight in grams
 */
public record ParcelDimensions(BigDecimal width, BigDecimal height, BigDecimal length, int weight) {

    public ParcelDimensions {
        Objects.requireNonNull(width, "width");
        Objects.requireNonNull(height, "height");
        Objects.requireNonNull(length, "length");
    }
}
