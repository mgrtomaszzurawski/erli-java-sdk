package io.github.mgrtomaszzurawski.erli.domain.inbox;

import io.github.mgrtomaszzurawski.erli.core.model.ProductExternalId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Payload of a {@link MessageType#PRODUCTS_NEED_SYNC} message: the products Erli wants the shop to
 * re-synchronise. An empty {@link #fields()} means the whole product, not "nothing".
 *
 * @param id         a hash of the product-id list. The spec marks it required, but a message observed
 *                   live on the sandbox omitted it, so it is optional here — see
 *                   {@code KNOWN-SERVER-BEHAVIORS.md}
 * @param productIds the products needing synchronisation
 * @param fields     the specific fields needing synchronisation, or empty for the whole product
 */
public record ProductsSyncEvent(
        Optional<String> id, List<ProductExternalId> productIds, List<String> fields)
        implements MessagePayload {

    public ProductsSyncEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(productIds, "productIds");
        Objects.requireNonNull(fields, "fields");
        productIds = List.copyOf(productIds);
        fields = List.copyOf(fields);
    }

    /** Whether the whole product needs re-synchronising rather than named fields. */
    public boolean wholeProduct() {
        return fields.isEmpty();
    }
}
