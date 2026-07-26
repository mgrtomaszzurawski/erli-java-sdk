package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of attaching or detaching products
 * ({@code PATCH /dictionaries/attachment/{attach,detach}}).
 *
 * <p><strong>Partial success is normal and is reported with HTTP 200.</strong> The API answers
 * {@code {"ok":false,"updated":[],"errors":[{"productId":…,"error":"…"}]}} when some products could
 * not be attached — an unknown product id, for instance — so a caller that only checks for an
 * exception would believe an attach that changed nothing had succeeded. Always check {@link #succeeded()}
 * or {@link #errors()}. (The published schema documents this operation as returning no body.)
 *
 * @param succeeded whether the API considered the whole operation successful
 * @param updatedProductIds the products whose attachment actually changed
 * @param errors one entry per product the API refused, verbatim
 */
public record ProductAttachmentResult(boolean succeeded, List<Long> updatedProductIds, List<ProductError> errors) {

    public ProductAttachmentResult {
        updatedProductIds = List.copyOf(Objects.requireNonNull(updatedProductIds, "updatedProductIds"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
    }

    /** Whether every requested product was updated. */
    public boolean isComplete() {
        return succeeded && errors.isEmpty();
    }

    /**
     * One product the API refused.
     *
     * @param productId the product that could not be updated, when the API names one
     * @param error the API's explanation, verbatim
     */
    public record ProductError(Optional<Long> productId, String error) {

        public ProductError {
            productId = productId == null ? Optional.empty() : productId;
        }
    }
}
