package io.github.mgrtomaszzurawski.erli.domain.shipping;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live end-to-end proof of the parcel slice against the Erli sandbox. Tagged {@code e2e}, so it is
 * excluded from the normal {@code test} run and executed by {@code ./gradlew :erli-client:e2eTest}
 * with {@code ERLI_BASE_URL} + {@code ERLI_API_KEY} set (from the shared secrets file).
 *
 * <p>The sandbox shop is empty, so no parcel exists to fetch by id yet. The proof available today is
 * the <em>not-found</em> path, and it exercises the whole chain regardless: base URL, Bearer
 * credential, request templating, live HTTP, and the server's real error body mapped to the
 * remediation exception. A stub cannot prove any of that. Once bucket A/D seed an order and this
 * bucket can create a parcel (Phase 3 live write→read), this test gains a happy-path sibling that
 * asserts mapped fields.
 */
@Tag("e2e")
class ParcelE2ETest {

    /** No parcel carries this id on the sandbox; the shop has none at all. */
    private static final String ABSENT_PARCEL_ID = "999999999";

    @Test
    void fetchingAnAbsentParcelReachesTheLiveApiAndMapsItsNotFoundError() {
        assumeTrue(isSet("ERLI_BASE_URL") && isSet("ERLI_API_KEY"), "sandbox environment not configured");

        try (ErliClient client = ErliClient.fromEnvironment()) {
            ShippingAccess shipping = client.shipping();

            ErliNotFoundException failure = assertThrows(
                    ErliNotFoundException.class, () -> shipping.parcel(ParcelId.of(ABSENT_PARCEL_ID)));

            // The credential was accepted (an auth failure would surface as ErliAuthException instead),
            // and the SDK preserved whatever the server actually sent rather than inventing a message.
            assertNotNull(failure.details(), "error details");
            assertNotNull(failure.details().rawBody(), "raw body from the live server");
        }
    }

    private static boolean isSet(String variable) {
        String value = System.getenv(variable);
        return value != null && !value.isBlank();
    }
}
