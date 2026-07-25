package io.github.mgrtomaszzurawski.erli.domain.shipping;

import java.util.Objects;
import java.util.Optional;

/**
 * A processing error recorded on a parcel by Erli or the carrier.
 *
 * <p>Distinct from the SDK's exception hierarchy: these arrive inside a successful {@code 200}
 * response and describe a parcel that exists but could not be registered, priced or labelled. Codes
 * come from the same Erli families used for failures (1100 server, 1200 validation, 1400 not-found);
 * the message is the carrier's or Erli's Polish text, preserved verbatim.
 *
 * @param errorCode    the Erli error code
 * @param errorMessage the human-readable message, when the API supplied one
 */
public record ParcelError(int errorCode, Optional<String> errorMessage) {

    public ParcelError {
        Objects.requireNonNull(errorMessage, "errorMessage");
    }
}
