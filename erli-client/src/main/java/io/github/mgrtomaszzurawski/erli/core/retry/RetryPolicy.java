package io.github.mgrtomaszzurawski.erli.core.retry;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Immutable retry configuration and backoff computation.
 *
 * <p>Retries are attempted on HTTP 429 and 5xx, and on transport failures, using an
 * <strong>equal-jitter</strong> exponential backoff (each wait is half a capped exponential window
 * plus a uniform random half — spreads retries without collapsing to zero). A {@code Retry-After}
 * hint from the server is treated as a <strong>floor</strong>: the effective wait is never shorter
 * than what the server asked for. By default {@code POST} is <strong>not</strong> retried
 * ({@code retryPost=false}), because Erli writes are not guaranteed idempotent.
 *
 * <p>The randomness source is injectable for deterministic tests; in production it defaults to
 * {@link ThreadLocalRandom} resolved on the calling thread.
 */
public final class RetryPolicy {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_BASE_DELAY = Duration.ofMillis(200);
    private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(20);

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_SERVER_ERROR_MIN = 500;
    private static final int HTTP_SERVER_ERROR_MAX = 599;

    private static final int SINGLE_ATTEMPT = 1;
    private static final int JITTER_HALVING_DIVISOR = 2;

    private final int maxAttempts;
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final boolean retryPost;
    private final RandomGenerator injectedRandom;

    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.baseDelay = builder.baseDelay;
        this.maxDelay = builder.maxDelay;
        this.retryPost = builder.retryPost;
        this.injectedRandom = builder.injectedRandom;
    }

    /** The default policy: up to 3 attempts, 200 ms base / 20 s cap, POST not retried. */
    public static RetryPolicy defaultPolicy() {
        return builder().build();
    }

    /** A policy that never retries (a single attempt). */
    public static RetryPolicy none() {
        return builder().maxAttempts(SINGLE_ATTEMPT).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Total number of attempts, including the first. A value of 1 disables retrying. */
    public int maxAttempts() {
        return maxAttempts;
    }

    /**
     * Whether a response with the given status is worth retrying for a request of the given method.
     *
     * @param httpStatus       the HTTP status of the failed response
     * @param idempotentMethod whether the request method is idempotent (GET/PUT/DELETE/HEAD)
     */
    public boolean isRetryableStatus(int httpStatus, boolean idempotentMethod) {
        boolean retryableStatus = httpStatus == HTTP_TOO_MANY_REQUESTS
                || (httpStatus >= HTTP_SERVER_ERROR_MIN && httpStatus <= HTTP_SERVER_ERROR_MAX);
        return retryableStatus && methodAllowsRetry(idempotentMethod);
    }

    /**
     * Whether a transport failure (I/O, timeout) is worth retrying for the given method. Idempotent
     * requests may be safely re-sent; a POST is retried only when {@code retryPost} is enabled.
     */
    public boolean isRetryableTransportFailure(boolean idempotentMethod) {
        return methodAllowsRetry(idempotentMethod);
    }

    private boolean methodAllowsRetry(boolean idempotentMethod) {
        return idempotentMethod || retryPost;
    }

    /**
     * The wait before the retry with the given index, using equal jitter and honoring a
     * {@code Retry-After} floor.
     *
     * @param retryIndex      0 for the first retry, 1 for the second, …
     * @param retryAfterFloor a server-provided minimum wait, or {@code null} if none
     * @return the duration to sleep before the next attempt
     */
    public Duration backoff(int retryIndex, Duration retryAfterFloor) {
        if (retryIndex < 0) {
            throw new IllegalArgumentException("retryIndex must be >= 0");
        }
        long cappedMillis = cappedExponentialMillis(retryIndex);
        long halfMillis = cappedMillis / JITTER_HALVING_DIVISOR;
        long jitterMillis = halfMillis == 0 ? 0 : randomGenerator().nextLong(halfMillis + 1);
        Duration jittered = Duration.ofMillis(halfMillis + jitterMillis);
        if (retryAfterFloor != null && retryAfterFloor.compareTo(jittered) > 0) {
            return retryAfterFloor;
        }
        return jittered;
    }

    private long cappedExponentialMillis(int retryIndex) {
        long baseMillis = baseDelay.toMillis();
        long capMillis = maxDelay.toMillis();
        long scaledMillis = baseMillis;
        for (int step = 0; step < retryIndex; step++) {
            scaledMillis <<= 1;
            if (scaledMillis >= capMillis || scaledMillis < 0) {
                return capMillis;
            }
        }
        return Math.min(scaledMillis, capMillis);
    }

    private RandomGenerator randomGenerator() {
        return injectedRandom != null ? injectedRandom : ThreadLocalRandom.current();
    }

    /** Builder for {@link RetryPolicy}. */
    public static final class Builder {

        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private Duration baseDelay = DEFAULT_BASE_DELAY;
        private Duration maxDelay = DEFAULT_MAX_DELAY;
        private boolean retryPost;
        private RandomGenerator injectedRandom;

        private Builder() {
        }

        public Builder maxAttempts(int value) {
            if (value < SINGLE_ATTEMPT) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            this.maxAttempts = value;
            return this;
        }

        public Builder baseDelay(Duration value) {
            this.baseDelay = requireNonNegative(value, "baseDelay");
            return this;
        }

        public Builder maxDelay(Duration value) {
            this.maxDelay = requireNonNegative(value, "maxDelay");
            return this;
        }

        public Builder retryPost(boolean value) {
            this.retryPost = value;
            return this;
        }

        /** Inject a deterministic randomness source (used by tests). */
        public Builder randomGenerator(RandomGenerator value) {
            this.injectedRandom = Objects.requireNonNull(value, "randomGenerator");
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }

        private static Duration requireNonNegative(Duration value, String field) {
            Objects.requireNonNull(value, field);
            if (value.isNegative()) {
                throw new IllegalArgumentException(field + " must not be negative");
            }
            return value;
        }
    }
}
