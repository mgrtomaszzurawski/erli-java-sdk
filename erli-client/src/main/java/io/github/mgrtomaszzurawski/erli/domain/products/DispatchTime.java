package io.github.mgrtomaszzurawski.erli.domain.products;

/**
 * How long the seller takes to hand a product to the carrier: a {@code period} counted in {@code unit}s.
 *
 * <p>{@code period} is required by the API; {@code unit} is optional and the marketplace reads a missing
 * one as {@link DispatchTimeUnit#DAY} (working days). That default is applied here so callers never see
 * an absent unit and never have to know the rule — the spec also notes that units other than days are
 * deprecated.
 *
 * @param unit   the time unit; {@link DispatchTimeUnit#DAY} when the API omits it
 * @param period the number of units
 */
public record DispatchTime(DispatchTimeUnit unit, int period) {

    /** The unit the marketplace assumes when a payload omits {@code unit}. */
    public static final DispatchTimeUnit DEFAULT_UNIT = DispatchTimeUnit.DAY;

    public DispatchTime {
        if (unit == null) {
            unit = DEFAULT_UNIT;
        }
        if (period < 0) {
            throw new IllegalArgumentException("dispatchTime period must not be negative, was " + period);
        }
    }

    /** A dispatch window of {@code period} working days — the marketplace default unit. */
    public static DispatchTime ofDays(int period) {
        return new DispatchTime(DispatchTimeUnit.DAY, period);
    }

    /** A dispatch window of {@code period} hours. */
    public static DispatchTime ofHours(int period) {
        return new DispatchTime(DispatchTimeUnit.HOUR, period);
    }

    /** A dispatch window of {@code period} months. */
    public static DispatchTime ofMonths(int period) {
        return new DispatchTime(DispatchTimeUnit.MONTH, period);
    }
}
