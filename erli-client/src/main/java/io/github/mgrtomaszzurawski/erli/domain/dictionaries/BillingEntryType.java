package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import java.util.Objects;

/**
 * A billing operation type, e.g. {@code COMM} — "naliczenie prowizji" (commission charge). Used to
 * interpret the {@code type} of a billing entry in the Finance domain.
 *
 * <p>The live API returns more fields than the published schema declares ({@code displayLabel},
 * {@code fiscalFunction}, {@code correctWith}, {@code invoiceType}); only the two specified fields are
 * modelled here. See {@code KNOWN-SERVER-BEHAVIORS.md}.
 *
 * @param type the operation code
 * @param description the Polish description of the operation
 */
public record BillingEntryType(String type, String description) {

    public BillingEntryType {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
    }
}
