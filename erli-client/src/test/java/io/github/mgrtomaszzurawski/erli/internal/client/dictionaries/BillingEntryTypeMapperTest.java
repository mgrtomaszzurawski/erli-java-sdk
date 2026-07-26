package io.github.mgrtomaszzurawski.erli.internal.client.dictionaries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mgrtomaszzurawski.erli.domain.dictionaries.BillingEntryType;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntryTypesResponseInner;
import org.junit.jupiter.api.Test;

class BillingEntryTypeMapperTest {

    /**
     * Verbatim from the live sandbox: {@code GET /dictionaries/billingEntryTypes}, 2026-07-25 (first
     * two of 48). The API also returns {@code displayLabel}, {@code fiscalFunction},
     * {@code correctWith} and {@code invoiceType}, which the published schema does not declare and
     * Layer 1 therefore drops — see KNOWN-SERVER-BEHAVIORS.
     */
    private static final String OBSERVED_BILLING_ENTRY_TYPES_JSON = """
            [
              {"type":"COMM","description":"naliczenie prowizji","displayLabel":"naliczenie prowizji",
               "fiscalFunction":"plusCharges","correctWith":"CORE","invoiceType":"P"},
              {"type":"COCR","description":"korekta prowizji","displayLabel":"korekta prowizji",
               "fiscalFunction":"plusCharges","invoiceType":"P"}
            ]""";

    private static BillingEntryTypesResponseInner[] decode(String json) {
        return new JsonCodec().read(json, BillingEntryTypesResponseInner[].class);
    }

    @Test
    void mapsTheDeclaredFieldsOfTheObservedPayload() {
        BillingEntryType commission = BillingEntryTypeMapper.toDomain(decode(OBSERVED_BILLING_ENTRY_TYPES_JSON)[0]);

        assertEquals("COMM", commission.type());
        assertEquals("naliczenie prowizji", commission.description());
    }

    @Test
    void ignoresTheUndeclaredFieldsTheApiAlsoReturns() {
        BillingEntryType correction = BillingEntryTypeMapper.toDomain(decode(OBSERVED_BILLING_ENTRY_TYPES_JSON)[1]);

        assertEquals("COCR", correction.type());
        assertEquals("korekta prowizji", correction.description());
    }

    @Test
    void rejectsAnEntryTypeMissingASpecRequiredField() {
        BillingEntryTypesResponseInner raw = decode("[{\"type\":\"COMM\"}]")[0];

        IllegalStateException failure =
                assertThrows(IllegalStateException.class, () -> BillingEntryTypeMapper.toDomain(raw));

        assertTrue(failure.getMessage().contains("'description'"), failure.getMessage());
    }
}
