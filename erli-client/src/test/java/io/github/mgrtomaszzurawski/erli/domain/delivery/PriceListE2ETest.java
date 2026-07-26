package io.github.mgrtomaszzurawski.erli.domain.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.error.ErliValidationException;
import io.github.mgrtomaszzurawski.erli.core.model.DeliveryMethodId;
import io.github.mgrtomaszzurawski.erli.core.model.Money;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live end-to-end proof of the delivery area against the Erli sandbox. Tagged {@code e2e}, so it is
 * excluded from the normal {@code test} run and executed by {@code ./gradlew :erli-client:e2eTest}
 * with {@code ERLI_BASE_URL} + {@code ERLI_API_KEY} set.
 *
 * <p>Read-only on purpose. Price lists are shop configuration, not sample data, so creating one would
 * leave the shared sandbox shop dirty for every other bucket; the write path is covered by
 * verify-on-write against WireMock and is scheduled for the Phase 3 live sweep.
 *
 * <p>Observed 2026-07-25: the sandbox shop has <em>no</em> price lists at all — not even the default
 * {@code "*"} one, and patching an absent one answers with a validation error rather than a 404. Both
 * are recorded in {@code KNOWN-SERVER-BEHAVIORS.md}.
 */
@Tag("e2e")
class PriceListE2ETest {

    /** No price list carries this id on the sandbox; the shop has none at all. */
    private static final long ABSENT_PRICE_LIST_ID = 999_999_999L;

    private static final DeliveryPrice SAMPLE_PRICE = new DeliveryPrice(
            new DeliveryMethodRef(DeliveryMethodId.of("erliPaczkomat"), Optional.empty()),
            Money.ofMinorUnits(1049, "PLN"), Money.ofMinorUnits(0, "PLN"), Optional.empty(), false);

    @Test
    void reachesTheLiveDeliveryEndpointsAndMapsAnAbsentPriceListToValidation() {
        assumeTrue(isSet("ERLI_BASE_URL") && isSet("ERLI_API_KEY"), "sandbox environment not configured");

        try (ErliClient client = ErliClient.fromEnvironment()) {
            List<PriceListSummary> summaries = client.delivery().priceLists();
            List<PriceList> details = client.delivery().priceListDetails(PriceListQuery.none());

            // The shop has no price lists configured (observed 2026-07-25), so both come back empty and
            // no assertion about their contents could prove anything. The load-bearing assertion is the
            // one below: a write to a price list that does not exist must come back as the mapped
            // exception. That fails for a wrong base URL (ErliTransportException), a rejected credential
            // (ErliAuthException) and a broken error mapping alike — none of which an emptiness check
            // would catch.
            //
            // Observed 2026-07-25: Erli answers an absent price list with a *validation* error, not a
            // 404. Pinned as observed rather than as the 404 one would expect; see
            // KNOWN-SERVER-BEHAVIORS.md.
            assertEquals(details.size(), summaries.size(), "both endpoints describe the same lists");

            var delivery = client.delivery();
            var absentListUpdate = PriceListUpdate.builder().price(SAMPLE_PRICE).build();
            ErliValidationException failure = assertThrows(ErliValidationException.class,
                    () -> delivery.updatePriceList(ABSENT_PRICE_LIST_ID, absentListUpdate));
            assertFalse(failure.details().rawBody().isBlank(), "live server sent an empty error body");
        }
    }

    private static boolean isSet(String variable) {
        String value = System.getenv(variable);
        return value != null && !value.isBlank();
    }
}
