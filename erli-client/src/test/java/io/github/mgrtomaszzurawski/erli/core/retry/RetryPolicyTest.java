package io.github.mgrtomaszzurawski.erli.core.retry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    private static final boolean IDEMPOTENT = true;
    private static final boolean NON_IDEMPOTENT = false;
    private static final long FIXED_SEED = 42L;

    @Test
    void retriesTooManyRequestsAndServerErrorsForIdempotentMethods() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        assertTrue(policy.isRetryableStatus(429, IDEMPOTENT));
        assertTrue(policy.isRetryableStatus(500, IDEMPOTENT));
        assertTrue(policy.isRetryableStatus(503, IDEMPOTENT));
        assertFalse(policy.isRetryableStatus(400, IDEMPOTENT));
        assertFalse(policy.isRetryableStatus(404, IDEMPOTENT));
    }

    @Test
    void doesNotRetryPostByDefaultButDoesWhenEnabled() {
        RetryPolicy defaultPolicy = RetryPolicy.defaultPolicy();
        assertFalse(defaultPolicy.isRetryableStatus(500, NON_IDEMPOTENT));
        assertFalse(defaultPolicy.isRetryableTransportFailure(NON_IDEMPOTENT));

        RetryPolicy postRetrying = RetryPolicy.builder().retryPost(true).build();
        assertTrue(postRetrying.isRetryableStatus(500, NON_IDEMPOTENT));
        assertTrue(postRetrying.isRetryableTransportFailure(NON_IDEMPOTENT));
    }

    @Test
    void noneMeansSingleAttempt() {
        assertEquals(1, RetryPolicy.none().maxAttempts());
    }

    @Test
    void equalJitterKeepsFirstBackoffBetweenHalfAndFullWindow() {
        Duration base = Duration.ofMillis(100);
        RetryPolicy policy = RetryPolicy.builder()
                .baseDelay(base)
                .maxDelay(Duration.ofSeconds(20))
                .randomGenerator(new Random(FIXED_SEED))
                .build();

        Duration backoff = policy.backoff(0, null);
        assertTrue(backoff.toMillis() >= 50, "equal jitter floor is half the window");
        assertTrue(backoff.toMillis() <= 100, "equal jitter ceiling is the full window");
    }

    @Test
    void backoffIsCappedAtMaxDelay() {
        RetryPolicy policy = RetryPolicy.builder()
                .baseDelay(Duration.ofMillis(100))
                .maxDelay(Duration.ofMillis(400))
                .randomGenerator(new Random(FIXED_SEED))
                .build();

        Duration backoff = policy.backoff(20, null);
        assertTrue(backoff.toMillis() <= 400, "never exceeds the configured cap");
    }

    @Test
    void retryAfterActsAsAFloor() {
        RetryPolicy policy = RetryPolicy.builder()
                .baseDelay(Duration.ofMillis(100))
                .randomGenerator(new Random(FIXED_SEED))
                .build();

        Duration floor = Duration.ofSeconds(5);
        assertEquals(floor, policy.backoff(0, floor), "server Retry-After wins when longer than jitter");
    }
}
