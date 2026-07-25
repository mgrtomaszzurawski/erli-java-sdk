package io.github.mgrtomaszzurawski.erli.demo;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.domain.orders.Order;
import io.github.mgrtomaszzurawski.erli.domain.orders.OrderSearchRequest;

import java.util.List;

/**
 * Live end-to-end proof of the Orders bucket: build a client from the environment
 * ({@code ERLI_BASE_URL} + {@code ERLI_API_KEY}) and search the real sandbox for orders.
 *
 * <p>Run: {@code ./gradlew :erli-demo:run -PmainClass=…ErliOrdersDemo} with both variables set
 * (sourced from {@code /workspace/shared/secrets/erli-sandbox.env}).
 *
 * <p>Nothing buyer-identifying is printed. {@link Order} renders personal data redacted, but this
 * runner also simply never reaches for it — the sandbox is a shared environment and its output ends up
 * in logs.
 */
public final class ErliOrdersDemo {

    private static final int SAMPLE_SIZE = 5;

    private ErliOrdersDemo() {
    }

    public static void main(String[] args) {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            List<Order> sample = client.orders()
                    .search(OrderSearchRequest.builder().pageSize(SAMPLE_SIZE).build())
                    .limit(SAMPLE_SIZE)
                    .toList();

            System.out.printf("POST /orders/_search OK -> %d order(s) on the first page%n", sample.size());
            for (Order order : sample) {
                System.out.printf(
                        "  %s status=%s sellerStatus=%s total=%s %s items=%d delivery=%s%n",
                        order.id(), order.status(), order.sellerStatus(),
                        order.totalPrice().amount(), order.totalPrice().currency(),
                        order.items().size(), order.delivery().typeId());
            }
            if (sample.isEmpty()) {
                System.out.println("  (the sandbox shop is empty — the round-trip and cursor walk still ran)");
            }
        }
    }
}
