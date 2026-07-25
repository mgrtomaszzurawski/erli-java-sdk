package io.github.mgrtomaszzurawski.erli.examples;

import io.github.mgrtomaszzurawski.erli.ErliClient;
import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliAuthException;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.shop.Shop;

import java.time.Duration;

/**
 * Compile-checked usage snippets for the public surface. These never run on a network; if a snippet
 * stops compiling, the public API changed in a breaking way — which is exactly what this module
 * guards. Only {@code sdk.core} and {@code sdk.domain.*} are imported here (never {@code internal}).
 */
public final class QuickStart {

    private QuickStart() {
    }

    /** The shortest path: read the key and base URL from the environment. */
    public static Shop fromEnvironment() {
        try (ErliClient client = ErliClient.fromEnvironment()) {
            return client.shop().me();
        }
    }

    /** Explicit configuration with a custom retry policy and timeouts. */
    public static Shop explicitlyConfigured(String baseUrl, String rawApiKey) {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxAttempts(4)
                .baseDelay(Duration.ofMillis(250))
                .maxDelay(Duration.ofSeconds(10))
                .build();

        try (ErliClient client = ErliClient.builder()
                .baseUrl(baseUrl)
                .apiKey(ApiKey.of(rawApiKey))
                .retryPolicy(retryPolicy)
                .requestTimeout(Duration.ofSeconds(20))
                .build()) {
            return client.shop().me();
        }
    }

    /** Handling a remediation-typed failure. */
    public static String describeAuthFailure(ErliClient client) {
        try {
            client.shop().me();
            return "ok";
        } catch (ErliAuthException authFailure) {
            return "check the API key: " + authFailure.details().message();
        }
    }
}
