package io.github.mgrtomaszzurawski.erli.demo;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DeliveryMethod;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Live end-to-end proof of the Dictionaries bucket: build a client from the environment
 * ({@code ERLI_BASE_URL} + {@code ERLI_API_KEY}) and read the delivery-method dictionary from the real
 * sandbox.
 *
 * <p>Dictionaries are marketplace-wide reference data, so this returns real payloads even though the
 * sandbox shop itself is empty — which is why bucket D doubles as the live-test enabler for the fleet.
 *
 * <p>Run: {@code ./gradlew :erli-demo:runDictionaries} with the two environment variables set (sourced
 * from {@code /workspace/shared/secrets/erli-sandbox.env}). The API key is never printed.
 */
public final class ErliDictionariesDemo {

    private static final int SAMPLE_SIZE = 5;

    private ErliDictionariesDemo() {
    }

    public static void main(String[] args) {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            List<DeliveryMethod> deliveryMethods = client.dictionaries().deliveryMethods();

            System.out.printf("GET /dictionaries/deliveryMethods OK -> %d methods%n", deliveryMethods.size());
            deliveryMethods.stream()
                    .limit(SAMPLE_SIZE)
                    .forEach(method -> System.out.printf(
                            "  %-28s %-45s cod=%-5b vendor=%s%n",
                            method.id(), method.name(), method.cashOnDelivery(), method.vendor()));

            String carriers = deliveryMethods.stream()
                    .map(method -> method.vendor().value())
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));
            System.out.printf("  distinct carriers: %s%n", carriers);
        }
    }
}
