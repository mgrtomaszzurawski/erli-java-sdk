package io.github.mgrtomaszzurawski.erli.domain.dictionaries;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live end-to-end proof of the delivery-methods slice against the Erli sandbox. Tagged {@code e2e}, so
 * it is excluded from the normal {@code test} run and executed by {@code ./gradlew :erli-client:e2eTest}
 * with {@code ERLI_BASE_URL} + {@code ERLI_API_KEY} set (from the shared secrets file). Delivery methods
 * are global reference data, so they are present even on the empty sandbox shop.
 */
@Tag("e2e")
class DeliveryMethodsE2ETest {

    @Test
    void listsDeliveryMethodsFromLiveSandbox() {
        assumeTrue(isSet("ERLI_BASE_URL") && isSet("ERLI_API_KEY"), "sandbox environment not configured");

        try (ErliClient client = ErliClient.fromEnvironment()) {
            List<DeliveryMethod> methods = client.dictionaries().deliveryMethods();

            assertFalse(methods.isEmpty(), "the sandbox should expose global delivery methods");
            DeliveryMethod first = methods.get(0);
            assertNotNull(first.id(), "id");
            assertNotNull(first.name(), "name");
            assertNotNull(first.vendor(), "vendor");
        }
    }

    private static boolean isSet(String variable) {
        String value = System.getenv(variable);
        return value != null && !value.isBlank();
    }
}
