package io.github.mgrtomaszzurawski.erli.domain.orders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The guards on the public request builders. Each one exists to turn a request Erli would reject —
 * or, worse, silently misinterpret — into an immediate, local failure with a message that says which
 * value was wrong.
 */
class OrderRequestValidationTest {

    private static final int OVER_LONG_EXTERNAL_ORDER_ID =
            OrderUpdateRequest.Builder.MAX_EXTERNAL_ORDER_ID_LENGTH + 1;

    @ParameterizedTest
    @ValueSource(ints = {0, -1, OrderSearchRequest.MAX_PAGE_SIZE + 1, Integer.MAX_VALUE})
    void rejectsAPageSizeOutsideTheRangeErliAccepts(int pageSize) {
        var builder = OrderSearchRequest.builder();
        assertThrows(IllegalArgumentException.class,
                () -> builder.pageSize(pageSize));
    }

    @ParameterizedTest
    @ValueSource(ints = {OrderSearchRequest.MIN_PAGE_SIZE, 50, OrderSearchRequest.MAX_PAGE_SIZE})
    void acceptsAPageSizeAtAndInsideTheBounds(int pageSize) {
        assertEquals(pageSize, OrderSearchRequest.builder().pageSize(pageSize).build().pageSize());
    }

    @Test
    void defaultsMatchWhatErliAppliesWhenNothingIsRequested() {
        OrderSearchRequest request = OrderSearchRequest.all();

        assertEquals(OrderSearchRequest.DEFAULT_PAGE_SIZE, request.pageSize());
        assertEquals(OrderSearchRequest.SortField.UPDATED, request.sortField());
        assertEquals(OrderSearchRequest.SortDirection.ASCENDING, request.direction());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsABlankExternalOrderId(String blank) {
        var builder = OrderUpdateRequest.builder();
        assertThrows(IllegalArgumentException.class,
                () -> builder.externalOrderId(blank));
    }

    @Test
    void rejectsAnExternalOrderIdLongerThanErliAllows() {
        String tooLong = "x".repeat(OVER_LONG_EXTERNAL_ORDER_ID);
        var builder = OrderUpdateRequest.builder();

        assertThrows(IllegalArgumentException.class,
                () -> builder.externalOrderId(tooLong));
    }

    @Test
    void acceptsAnExternalOrderIdExactlyAtTheLimit() {
        String atLimit = "x".repeat(OrderUpdateRequest.Builder.MAX_EXTERNAL_ORDER_ID_LENGTH);

        assertDoesNotThrow(() -> OrderUpdateRequest.ofExternalOrderId(atLimit));
    }

    @Test
    void reportsAnUpdateThatWouldChangeNothingAsEmpty() {
        assertEquals(true, OrderUpdateRequest.builder().build().isEmpty());
        assertEquals(false, OrderUpdateRequest.ofExternalOrderId("erp-1").isEmpty());
    }

    @Test
    void rejectsAMembershipFilterWithNoValues() {
        List<String> noValues = List.of();
        assertThrows(IllegalArgumentException.class,
                () -> OrderFilter.in(OrderFilter.Field.ID, noValues));
        assertThrows(IllegalArgumentException.class,
                () -> OrderFilter.notIn(OrderFilter.Field.ID, noValues));
    }

    @Test
    void rejectsALogicalCombinationWithNoOperands() {
        assertThrows(IllegalArgumentException.class, OrderFilter::and);
        assertThrows(IllegalArgumentException.class, OrderFilter::or);
    }

    @Test
    void filterRecordsAreDefensivelyCopied() {
        List<String> mutable = new java.util.ArrayList<>(List.of("221201x1"));
        OrderFilter.Membership filter =
                (OrderFilter.Membership) OrderFilter.in(OrderFilter.Field.ID, mutable);

        mutable.add("221201x2");

        assertEquals(1, filter.values().size());
        var immutableValues = filter.values();
        assertThrows(UnsupportedOperationException.class, () -> immutableValues.clear());
    }
}
