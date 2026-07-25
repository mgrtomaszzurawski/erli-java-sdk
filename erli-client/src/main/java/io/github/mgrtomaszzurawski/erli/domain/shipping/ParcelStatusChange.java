package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * One entry of a parcel's status history: the status it moved to and when.
 *
 * @param status  the status entered at this point
 * @param changed when the transition happened
 */
public record ParcelStatusChange(ParcelStatus status, OffsetDateTime changed) {

    public ParcelStatusChange {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(changed, "changed");
    }
}
