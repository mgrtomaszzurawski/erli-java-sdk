package io.github.mgrtomaszzurawski.erli.demo;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.domain.hooks.Hook;
import io.github.mgrtomaszzurawski.erli.domain.inbox.Message;
import io.github.mgrtomaszzurawski.erli.domain.inbox.MessageQuery;

import java.util.List;

/**
 * Live end-to-end proof of the Comms &amp; Automation bucket against the real sandbox: reads the shop's
 * webhook subscriptions and drains-checks its inbox through the public accessors.
 *
 * <p>Run: {@code ./gradlew :erli-demo:runComms} with {@code ERLI_BASE_URL} and {@code ERLI_API_KEY} set
 * (sourced from {@code /workspace/shared/secrets/erli-sandbox.env}). The API key is never printed.
 *
 * <p>Read-only by design. The write operations ({@code save}/{@code delete} a subscription, the two
 * hook test-fires, {@code mark-read}) change shop state or make Erli call an endpoint the sandbox shop
 * does not host, so they are exercised against WireMock and left to the Phase 3 live write→read sweep.
 * Buyer personal data is never printed: the domain records redact themselves.
 */
public final class ErliCommsDemo {

    private ErliCommsDemo() {
    }

    public static void main(String[] args) {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            List<Hook> hooks = client.hooks().list();
            System.out.printf("GET /hooks OK -> %d subscription(s)%n", hooks.size());
            hooks.forEach(hook -> System.out.printf("  %s -> %s (accessToken %s)%n",
                    hook.kind(), hook.url(), hook.accessToken().isPresent() ? "set" : "unset"));

            List<Message> unread = client.inbox().unread();
            System.out.printf("GET /inbox OK -> %d unread message(s)%n", unread.size());
            unread.forEach(message -> System.out.printf("  %s %s created=%s payload=%s%n",
                    message.id(), message.type(), message.created(),
                    message.payload().map(payload -> payload.getClass().getSimpleName()).orElse("unmapped")));

            List<Message> filtered = client.inbox().search(MessageQuery.all());
            System.out.printf("POST /inbox/_search OK -> %d unread message(s)%n", filtered.size());
        }
    }
}
