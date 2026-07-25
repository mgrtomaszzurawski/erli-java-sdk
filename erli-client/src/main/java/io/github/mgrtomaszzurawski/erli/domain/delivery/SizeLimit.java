package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.util.Objects;

/**
 * How many items fit in one parcel of a given locker size bracket.
 *
 * @param dimension the size bracket
 * @param limit     maximum number of items for that bracket
 */
public record SizeLimit(ParcelSize dimension, int limit) {

    public SizeLimit {
        Objects.requireNonNull(dimension, "dimension");
    }
}
