package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesRequest;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesRequestSimpleFilter;
import io.github.mgrtomaszzurawski.erli.rest.model.BillingEntriesResponseInner;
import io.github.mgrtomaszzurawski.erli.rest.model.ShopResponse;
import io.github.mgrtomaszzurawski.erli.rest.model.Transaction;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codec-level contract every bucket depends on. The Core M1 shop payload is all scalars, so three
 * shapes went unexercised until later buckets needed them: a {@code date-time} property, an
 * openapi-generator {@code JsonNullable} property, and a request body with an unset optional.
 *
 * <p>Fixtures using billing and transaction shapes are hand-built from the vendored
 * {@code openapi/swagger.json} schemas named in each constant — the sandbox shop is empty, so no real
 * payload exists to copy yet (see {@code TESTING.md} "Fixture provenance").
 */
class JsonCodecTest {

    /** Hand-built from the {@code BillingEntriesResponse} item schema; timestamp in the API's offset. */
    private static final String BILLING_ENTRY_JSON = """
            {"id":42,"occurredAt":"2026-07-24T14:45:47.540+02:00","type":"COMMISSION",
             "description":"Prowizja","amount":-1168,"balanceAfter":5000}
            """;

    /** Hand-built from the {@code Transaction} schema; {@code balanceSnapshot} is its JsonNullable field. */
    private static final String TRANSACTION_WITH_SNAPSHOT_JSON = """
            {"type":"PAYOUT","status":"DONE","amount":1000,"balanceSnapshot":{"available":250},
             "erliCreationDate":"2026-07-24T14:45:47.540+02:00"}
            """;

    /** Same schema with {@code balanceSnapshot} explicitly null. */
    private static final String TRANSACTION_WITH_NULL_SNAPSHOT_JSON = """
            {"type":"PAYOUT","balanceSnapshot":null}
            """;

    /** Same schema with {@code balanceSnapshot} absent entirely. */
    private static final String TRANSACTION_WITHOUT_SNAPSHOT_JSON = """
            {"type":"PAYOUT"}
            """;

    private static final String EXPECTED_TIMESTAMP = "2026-07-24T14:45:47.540+02:00";
    private static final String BALANCE_SNAPSHOT_PROPERTY = "balanceSnapshot";
    private static final String PAGINATION_PROPERTY = "pagination";
    private static final String ORDER_ID_VALUE = "202607x1234";

    private final JsonCodec codec = new JsonCodec();

    @Test
    void readListDecodesEachElement() {
        String body = "[{\"id\":1,\"name\":\"a\"},{\"id\":2,\"name\":\"b\"}]";

        List<ShopResponse> shops = codec.readList(body, ShopResponse.class);

        assertEquals(2, shops.size());
        assertEquals("a", shops.get(0).getName());
        assertEquals(2, shops.get(1).getId().intValue());
    }

    @Test
    void readListDecodesEmptyArray() {
        assertTrue(codec.readList("[]", ShopResponse.class).isEmpty());
    }

    @Test
    void readIgnoresUnknownProperties() {
        ShopResponse shop = codec.read("{\"id\":7,\"name\":\"n\",\"surprise\":true}", ShopResponse.class);
        assertEquals(7, shop.getId().intValue());
    }

    @Test
    void readWrapsMalformedJsonAsTransportError() {
        assertThrows(ErliTransportException.class, () -> codec.read("{not json", ShopResponse.class));
    }

    @Test
    void readTreeLenientReturnsNullForNonJson() {
        assertNull(codec.readTreeLenient("<html>nope</html>"));
        assertNull(codec.readTreeLenient(""));
    }

    @Test
    void decodesDateTimePropertiesPreservingTheOffset() {
        BillingEntriesResponseInner entry = codec.read(BILLING_ENTRY_JSON, BillingEntriesResponseInner.class);

        // Whole-value assertion: a wrong offset or dropped millis must fail, not just a wrong year.
        assertEquals(OffsetDateTime.parse(EXPECTED_TIMESTAMP), entry.getOccurredAt());
    }

    @Test
    void decodesAJsonNullablePropertyThatCarriesAValue() {
        Transaction transaction = codec.read(TRANSACTION_WITH_SNAPSHOT_JSON, Transaction.class);

        // Without JsonNullableModule this non-null value throws InvalidDefinitionException.
        assertTrue(transaction.getBalanceSnapshot_JsonNullable().isPresent());
        assertEquals(OffsetDateTime.parse(EXPECTED_TIMESTAMP), transaction.getErliCreationDate());
    }

    @Test
    void decodesAnExplicitlyNullJsonNullableAsPresentAndNull() {
        Transaction explicitNull = codec.read(TRANSACTION_WITH_NULL_SNAPSHOT_JSON, Transaction.class);
        Transaction absent = codec.read(TRANSACTION_WITHOUT_SNAPSHOT_JSON, Transaction.class);

        assertTrue(explicitNull.getBalanceSnapshot_JsonNullable().isPresent(),
                "an explicit JSON null must decode as present-and-null");
        assertNull(explicitNull.getBalanceSnapshot());
        // Pinning a generator quirk rather than an aspiration: openapi-generator initialises this
        // field to JsonNullable.of(null), not undefined(), so an ABSENT key is indistinguishable
        // from an explicit null on a response model. Mappers must therefore treat both as "no
        // value" and must not infer "the server explicitly cleared this" from isPresent().
        assertTrue(absent.getBalanceSnapshot_JsonNullable().isPresent(),
                "generator initialises the field to of(null), so absence also reads as present");
        assertNull(absent.getBalanceSnapshot());
    }

    @Test
    void omitsUnsetOptionalsWhenWritingARequestBody() {
        // The API rejects an explicitly-null optional instead of treating it as absent, and on a
        // PATCH an explicit null clears the field (KNOWN-SERVER-BEHAVIORS.md, observed 2026-07-25).
        BillingEntriesRequest request = new BillingEntriesRequest()
                .simpleFilter(new BillingEntriesRequestSimpleFilter().orderId(ORDER_ID_VALUE));

        String json = codec.write(request);

        assertFalse(json.contains(PAGINATION_PROPERTY),
                "unset 'pagination' must be omitted, not written as null: " + json);
        assertFalse(json.contains("null"), "no property may be written as an explicit null: " + json);
        assertTrue(json.contains(ORDER_ID_VALUE));
    }

    @Test
    void writesDateTimesAsIso8601StringsNotEpochNumbers() {
        Transaction transaction = new Transaction().erliCreationDate(OffsetDateTime.parse(EXPECTED_TIMESTAMP));

        String json = codec.write(transaction);

        assertTrue(json.contains("2026-07-24T14:45:47.54"),
                "date-times must serialize as RFC 3339 text: " + json);
    }

    @Test
    void writesAnExplicitlyNullJsonNullableEvenThoughUnsetFieldsAreOmitted() {
        // NON_NULL must not defeat the deliberate "set this field to null" idiom that PATCH-style
        // request models rely on: undefined is dropped, explicit null is kept.
        Transaction transaction = new Transaction().balanceSnapshot(null);

        String json = codec.write(transaction);

        assertTrue(json.contains(BALANCE_SNAPSHOT_PROPERTY),
                "an explicitly-null JsonNullable must survive NON_NULL: " + json);
    }
}
