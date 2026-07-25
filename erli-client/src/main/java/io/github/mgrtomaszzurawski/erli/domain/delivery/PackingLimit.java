package io.github.mgrtomaszzurawski.erli.domain.delivery;

import java.util.List;
import java.util.Objects;

/**
 * How many items the seller allows in one parcel under a given price entry.
 *
 * <p>The API states this as a {@code oneOf}: a plain count for most delivery methods, or a per-bracket
 * table for {@code erliPaczkomat}, where an InPost locker's A/B/C compartments hold different amounts.
 * Layer 1 cannot express that (the generator collapses the array branch to a free-form object), so the
 * domain re-types it as a sealed pair — a caller pattern-matches instead of inspecting a raw map, and
 * an unhandled branch is a compile error.
 */
public sealed interface PackingLimit permits PackingLimit.Total, PackingLimit.PerSize {

    /** A single cap that applies whatever the parcel's size bracket. */
    record Total(int maxItems) implements PackingLimit {
    }

    /**
     * One cap per locker size bracket. The API requires all three brackets for {@code erliPaczkomat}.
     *
     * @param limits the per-bracket caps, in the order the API stated them
     */
    record PerSize(List<SizeLimit> limits) implements PackingLimit {

        public PerSize {
            limits = List.copyOf(Objects.requireNonNull(limits, "limits"));
        }
    }
}
