package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Physical size and weight of a parcel, as the carrier prices it.
 *
 * <p><strong>Units are millimetres and grams</strong>, as the API states them — not centimetres. The
 * spec bounds each edge to 1–2000 mm and the weight to 10–700000 g. {@link BigDecimal} keeps the edges
 * exactly as received: carrier size brackets are compared against these values, so binary rounding must
 * not creep in.
 *
 * @param width  width in millimetres
 * @param height height in millimetres
 * @param length length in millimetres
 * @param weight weight in grams
 */
public record ParcelDimensions(BigDecimal width, BigDecimal height, BigDecimal length, int weight) {

    public ParcelDimensions {
        Objects.requireNonNull(width, "width");
        Objects.requireNonNull(height, "height");
        Objects.requireNonNull(length, "length");
    }
}
