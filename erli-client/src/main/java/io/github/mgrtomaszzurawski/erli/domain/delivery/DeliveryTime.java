package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.util.Objects;

/**
 * The delivery window a method promises, as a range in one unit.
 *
 * @param unit      whether the window is counted in days or hours
 * @param minPeriod fastest expected delivery, in {@code unit}
 * @param maxPeriod slowest expected delivery, in {@code unit}
 */
public record DeliveryTime(DeliveryTimeUnit unit, int minPeriod, int maxPeriod) {

    public DeliveryTime {
        Objects.requireNonNull(unit, "unit");
    }
}
