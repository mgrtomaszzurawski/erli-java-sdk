package io.github.mgrtomaszzurawski.erli.internal.client.orders;

import io.github.mgrtomaszzurawski.erli.core.model.SellerStatus;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderFilter;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderSearchRequest;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Request-mapping contract: every domain-to-wire token the SDK can emit.
 *
 * <p>These are hand-written translations, so a swapped {@code >=}/{@code <=} or a mistyped status
 * would be invisible to a response-side test and would only surface as the wrong orders coming back —
 * or, on {@code changeStatus}, as the wrong status written to a real order. Asserted against the
 * serialized body, which is what actually goes on the wire.
 */
class OrderRequestMapperTest {

    private static final OffsetDateTime MOMENT =
            OffsetDateTime.of(2026, 7, 20, 8, 15, 30, 0, ZoneOffset.UTC);

    private final JsonCodec codec = new JsonCodec();

    private String searchBodyWith(OrderFilter filter) {
        return codec.write(OrderRequestMapper.toSearchBody(
                OrderSearchRequest.builder().filter(filter).build(), null));
    }

    @ParameterizedTest(name = "{0} serializes as {1}")
    @CsvSource({
            "EQUAL,                  '='",
            "NOT_EQUAL,              '!='",
            "GREATER_THAN,           '>'",
            "GREATER_THAN_OR_EQUAL,  '>='",
            "LESS_THAN,              '<'",
            "LESS_THAN_OR_EQUAL,     '<='",
    })
    void mapsEveryComparisonOperator(OrderFilter.ComparisonOperator operator, String expectedToken) {
        String body = searchBodyWith(OrderFilter.compare(OrderFilter.Field.ID, operator, "221201x1"));

        assertTrue(body.contains("\"operator\":\"" + expectedToken + "\""),
                "expected operator " + expectedToken + " in: " + body);
    }

    @ParameterizedTest(name = "{0} serializes as {1}")
    @CsvSource({"IN, in", "NOT_IN, nin"})
    void mapsBothMembershipOperators(OrderFilter.MembershipOperator operator, String expectedToken) {
        OrderFilter filter = operator == OrderFilter.MembershipOperator.IN
                ? OrderFilter.in(OrderFilter.Field.ID, List.of("221201x1"))
                : OrderFilter.notIn(OrderFilter.Field.ID, List.of("221201x1"));

        assertTrue(searchBodyWith(filter).contains("\"operator\":\"" + expectedToken + "\""));
    }

    @ParameterizedTest(name = "{0} serializes as {1}")
    @CsvSource({"ID, id", "CREATED, created", "UPDATED, updated",
            "PAYMENT_STATUS, paymentStatus", "USER_EMAIL, userEmail"})
    void mapsEveryFilterableField(OrderFilter.Field field, String expectedToken) {
        String body = searchBodyWith(OrderFilter.equalTo(field, "value"));

        assertTrue(body.contains("\"field\":\"" + expectedToken + "\""),
                "expected field " + expectedToken + " in: " + body);
    }

    @Test
    void formatsCreatedAfterAsAnIsoOffsetDateTime() {
        String body = searchBodyWith(OrderFilter.createdAfter(MOMENT));

        assertTrue(body.contains("\"field\":\"created\""), body);
        assertTrue(body.contains("\"operator\":\">\""), body);
        assertTrue(body.contains("\"value\":\"2026-07-20T08:15:30Z\""), body);
    }

    @Test
    void formatsUpdatedAfterAsAnIsoOffsetDateTime() {
        // The headline incremental-sync filter: a wrong format here silently re-reads or skips orders.
        String body = searchBodyWith(OrderFilter.updatedAfter(MOMENT));

        assertTrue(body.contains("\"field\":\"updated\""), body);
        assertTrue(body.contains("\"value\":\"2026-07-20T08:15:30Z\""), body);
    }

    @Test
    void preservesANonUtcOffsetWhenFormattingAFilterValue() {
        OffsetDateTime warsawSummer = OffsetDateTime.of(2026, 7, 20, 10, 15, 30, 0, ZoneOffset.ofHours(2));

        assertTrue(searchBodyWith(OrderFilter.updatedAfter(warsawSummer))
                .contains("\"value\":\"2026-07-20T10:15:30+02:00\""));
    }

    @Test
    void mapsPaymentCompletedToTheOnlyValueErliAccepts() {
        String body = searchBodyWith(OrderFilter.paymentCompleted());

        assertTrue(body.contains("\"field\":\"paymentStatus\""), body);
        assertTrue(body.contains("\"value\":\"completed\""), body);
    }

    @Test
    void mapsLogicalCombinatorsAndNegation() {
        String body = searchBodyWith(OrderFilter.and(
                OrderFilter.or(OrderFilter.paymentCompleted(), OrderFilter.userEmail("a@example.com")),
                OrderFilter.not(OrderFilter.equalTo(OrderFilter.Field.ID, "221201x1"))));

        assertTrue(body.contains("\"operator\":\"and\""), body);
        assertTrue(body.contains("\"operator\":\"or\""), body);
        assertTrue(body.contains("\"operator\":\"not\""), body);
    }

    @ParameterizedTest
    @EnumSource(SellerStatus.class)
    void mapsEverySellerStatusToItsWireToken(SellerStatus status) {
        String body = codec.write(OrderRequestMapper.toStatusBody(status));

        // Erli's tokens are lowerCamelCase of the constant name; assert the exact token, not a match
        // against the same transformation the mapper would use.
        String expected = switch (status) {
            case CREATED -> "created";
            case CANCELED -> "canceled";
            case READY_TO_PROCESS -> "readyToProcess";
            case IN_PROGRESS -> "inProgress";
            case SENT -> "sent";
            case READY_TO_PICKUP -> "readyToPickup";
            case RECEIVED -> "received";
            case RETURNED -> "returned";
            case RETURNING_TO_SENDER -> "returningToSender";
            case UNKNOWN -> "unknown";
        };
        assertEquals("{\"status\":\"" + expected + "\"}", body);
    }

    @ParameterizedTest(name = "{0} serializes as {1}")
    @CsvSource({"CREATED, created", "UPDATED, updated"})
    void mapsBothSortFields(OrderSearchRequest.SortField sortField, String expectedToken) {
        String body = codec.write(OrderRequestMapper.toSearchBody(
                OrderSearchRequest.builder().sortBy(sortField).build(), null));

        assertTrue(body.contains("\"sortField\":\"" + expectedToken + "\""), body);
    }

    @ParameterizedTest(name = "{0} serializes as {1}")
    @CsvSource({"ASCENDING, ASC", "DESCENDING, DESC"})
    void mapsBothSortDirections(OrderSearchRequest.SortDirection direction, String expectedToken) {
        String body = codec.write(OrderRequestMapper.toSearchBody(
                OrderSearchRequest.builder().direction(direction).build(), null));

        assertTrue(body.contains("\"order\":\"" + expectedToken + "\""), body);
    }

    @Test
    void theWalkCursorWinsOverTheRequestsOwnStartCursor() {
        String body = codec.write(OrderRequestMapper.toSearchBody(
                OrderSearchRequest.builder()
                        .startAfter(io.github.mgrtomaszzurawski.erli.core.model.Cursor.of("first"))
                        .build(),
                io.github.mgrtomaszzurawski.erli.core.model.Cursor.of("later")));

        assertTrue(body.contains("\"after\":\"later\""), body);
    }
}
