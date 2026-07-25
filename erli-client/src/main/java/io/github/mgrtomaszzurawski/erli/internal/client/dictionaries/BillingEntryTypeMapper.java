package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.BillingEntryType;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntryTypesResponseInner;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Maps the generated Layer-1 billing entry type to the public {@link BillingEntryType} domain record.
 * Internal: never exported.
 */
final class BillingEntryTypeMapper {

    private BillingEntryTypeMapper() {
    }

    static List<BillingEntryType> toDomainList(BillingEntryTypesResponseInner[] rawTypes) {
        if (rawTypes == null) {
            return List.of();
        }
        return Arrays.stream(rawTypes).map(BillingEntryTypeMapper::toDomain).toList();
    }

    static BillingEntryType toDomain(BillingEntryTypesResponseInner rawType) {
        Objects.requireNonNull(rawType, "raw BillingEntryTypesResponseInner");
        return new BillingEntryType(
                require(rawType.getType(), "type"),
                require(rawType.getDescription(), "description"));
    }

    private static String require(String value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException(
                    "BillingEntryType is missing the required '" + fieldName + "' field");
        }
        return value;
    }
}
