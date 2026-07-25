package io.github.mgrtomaszzurawski.erli.domain.products;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.util.Optional;

/**
 * The result for one product in a batch update ({@code PATCH /products/batch-update}).
 *
 * <p>A batch call succeeds as a whole while individual entries fail, so each entry carries its own HTTP
 * status: {@code 202} means accepted, anything else means that one product was rejected and
 * {@link #error()} says why. Reading only the call's own status would hide those failures.
 *
 * @param externalId the product this outcome is for
 * @param status     the per-entry HTTP status
 * @param result     what changed, when the entry was accepted
 * @param error      why it was rejected, when it was not
 */
public record BatchUpdateOutcome(
        ProductExternalId externalId,
        int status,
        Optional<ProductUpdateResult> result,
        Optional<BatchUpdateError> error) {

    /** The per-entry status the marketplace returns for an accepted update. */
    public static final int ACCEPTED_STATUS = 202;

    /** Whether the marketplace accepted this entry. */
    public boolean isAccepted() {
        return status == ACCEPTED_STATUS && error.isEmpty();
    }
}
