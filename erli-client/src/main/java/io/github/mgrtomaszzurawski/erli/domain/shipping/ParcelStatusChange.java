package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * One entry of a parcel's status history: the status it moved to and, when the API states it, the
 * moment of the transition. Only the status is spec-required — carrier-sourced entries can arrive
 * without a timestamp, and a read must not fail on a payload the server is entitled to send.
 *
 * @param status  the status entered at this point
 * @param changed when the transition happened, when the API supplied it
 */
public record ParcelStatusChange(ParcelStatus status, Optional<OffsetDateTime> changed) {

    public ParcelStatusChange {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(changed, "changed");
    }
}
