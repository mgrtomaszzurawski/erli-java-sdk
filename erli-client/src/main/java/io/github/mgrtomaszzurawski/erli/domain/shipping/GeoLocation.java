package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Where a pickup point physically is. {@link BigDecimal} keeps the coordinates exactly as the API
 * stated them — rounding a coordinate moves the point.
 *
 * @param latitude  degrees north
 * @param longitude degrees east
 */
public record GeoLocation(BigDecimal latitude, BigDecimal longitude) {

    public GeoLocation {
        Objects.requireNonNull(latitude, "latitude");
        Objects.requireNonNull(longitude, "longitude");
    }
}
