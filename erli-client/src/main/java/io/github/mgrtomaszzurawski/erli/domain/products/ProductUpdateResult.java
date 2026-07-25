package io.github.mgrtomaszzurawski.erli.domain.products;

import java.util.Set;

/**
 * What an update actually changed ({@code PATCH /products/{externalId}}).
 *
 * <p>Worth checking rather than assuming: the marketplace ignores writes to
 * {@linkplain FrozenFields frozen} fields, so a field you sent may be absent here. Fields the
 * marketplace named but this SDK version does not know are dropped from {@link #updatedFields()} and
 * kept in {@link #unrecognisedFields()}, so a newer API never turns a successful update into an error.
 *
 * @param updatedFields      the fields the marketplace reports as changed
 * @param unrecognisedFields wire names this SDK version does not map
 */
public record ProductUpdateResult(Set<ProductField> updatedFields, Set<String> unrecognisedFields) {

    public ProductUpdateResult {
        updatedFields = updatedFields.isEmpty() ? Set.of() : Set.copyOf(updatedFields);
        unrecognisedFields = unrecognisedFields.isEmpty() ? Set.of() : Set.copyOf(unrecognisedFields);
    }

    /** Whether the marketplace reports the given field as changed. */
    public boolean changed(ProductField field) {
        return updatedFields.contains(field);
    }
}
