package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * The body {@code PATCH /dictionaries/attachment/{attach,detach}} really returns.
 *
 * <p>Hand-written rather than generated because the published spec documents this operation as
 * answering with <em>no body</em>. Observed live on the sandbox (2026-07-25):
 * {@code {"ok":false,"updated":[],"errors":[{"productId":999999999,"error":"NotFoundFailure: …"}]}},
 * returned with HTTP 200 — so a caller that only watches for an exception cannot tell that nothing
 * was attached. Layer 1 has no type for it, so this is the one place in the bucket where a response
 * DTO is hand-written; it is mapped straight into the public
 * {@link io.github.mgrtomaszzurawski.erli.domain.dictionaries.ProductAttachmentResult}.
 *
 * <p>Internal: never exported.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
final class ManageAttachedProductsResponse {

    private Boolean ok;
    private List<Integer> updated;
    private List<ProductErrorResponse> errors;

    Boolean getOk() {
        return ok;
    }

    void setOk(Boolean ok) {
        this.ok = ok;
    }

    List<Integer> getUpdated() {
        return updated;
    }

    void setUpdated(List<Integer> updated) {
        this.updated = updated;
    }

    List<ProductErrorResponse> getErrors() {
        return errors;
    }

    void setErrors(List<ProductErrorResponse> errors) {
        this.errors = errors;
    }

    /** One refused product, as the API reports it. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class ProductErrorResponse {

        private Integer productId;
        private String error;

        Integer getProductId() {
            return productId;
        }

        void setProductId(Integer productId) {
            this.productId = productId;
        }

        String getError() {
            return error;
        }

        void setError(String error) {
            this.error = error;
        }
    }
}
