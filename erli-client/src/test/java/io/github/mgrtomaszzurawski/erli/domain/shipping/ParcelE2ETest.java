package io.github.mgrtomaszzurawski.erli.domain.shipping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.error.ErliNotFoundException;
import io.github.mgrtomaszzurawski.erli.core.model.ParcelId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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

            // The exception type IS the assertion: a wrong base URL would surface as
            // ErliTransportException and a rejected credential as ErliAuthException, so reaching
            // "not found" proves the host resolved, TLS completed and the Bearer key was accepted.
            ParcelId absentParcelId = ParcelId.of(ABSENT_PARCEL_ID);
            ErliNotFoundException failure = assertThrows(
                    ErliNotFoundException.class, () -> shipping.parcel(absentParcelId));

            // The server answered with a body of its own rather than the SDK inventing one. Asserting
            // non-emptiness, not merely non-null: `rawBody` is never null by construction, so a null
            // check here would pass no matter what came back.
            assertFalse(failure.details().rawBody().isBlank(), "live server sent an empty error body");
        }
    }

    private static boolean isSet(String variable) {
        String value = System.getenv(variable);
        return value != null && !value.isBlank();
    }
}
