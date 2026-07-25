package io.github.mgrtomaszzurawski.erli;

import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliConfigurationException;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;
import io.github.mgrtomaszzurawski.erli.domain.dictionaries.DictionaryAccess;
import io.github.mgrtomaszzurawski.erli.domain.shop.ShopAccess;
import io.github.mgrtomaszzurawski.erli.internal.ErrorMapper;
import io.github.mgrtomaszzurawski.erli.internal.HttpRuntime;
import io.github.mgrtomaszzurawski.erli.internal.JsonCodec;
import io.github.mgrtomaszzurawski.erli.internal.client.dictionaries.DictionaryAccessImpl;
import io.github.mgrtomaszzurawski.erli.internal.client.shop.ShopAccessImpl;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point to the Erli Marketplace SDK. Build one with {@link #builder()} (or
 * {@link #fromEnvironment()}), reach a domain through its accessor (e.g. {@link #shop()}), and
 * {@link #close()} it when done. A closed client rejects further use.
 *
 * <p>Instances are immutable and safe to share across threads; the underlying
 * {@link java.net.http.HttpClient} pools and reuses connections. Domain accessors are the second of
 * the three shared append points — each bucket adds its accessor under the reserved block below.
 */
public final class ErliClient implements AutoCloseable {

    /** Environment variable holding the API base URL (e.g. {@code https://sandbox.erli.dev/svc/shop-api}). */
    public static final String BASE_URL_ENV_VAR = "ERLI_BASE_URL";

    private static final String SDK_VERSION = "0.0.1-SNAPSHOT";
    private static final String DEFAULT_USER_AGENT = "erli-java-sdk/" + SDK_VERSION;
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ShopAccess shop;
    private final DictionaryAccess dictionaries;

    private ErliClient(Builder builder) {
        HttpClient httpClient = builder.httpClient != null
                ? builder.httpClient
                : HttpClient.newBuilder().connectTimeout(builder.connectTimeout).build();
        JsonCodec codec = new JsonCodec();
        HttpRuntime runtime = new HttpRuntime(
                httpClient,
                builder.baseUrl,
                builder.apiKey,
                builder.retryPolicy,
                builder.userAgent,
                builder.requestTimeout,
                codec,
                new ErrorMapper(codec));
        this.shop = new ShopAccessImpl(runtime);
        this.dictionaries = new DictionaryAccessImpl(runtime);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Build a client reading both the API key and the base URL from the environment. */
    public static ErliClient fromEnvironment() {
        return builder()
                .apiKey(ApiKey.fromEnvironment())
                .baseUrlFromEnvironment()
                .build();
    }

    /** Access to the authenticated shop ({@code GET /me}). */
    public ShopAccess shop() {
        ensureOpen();
        return shop;
    }

    // --- APPEND BLOCK: bucket A Products accessor -------------------------------------------------
    // --- APPEND BLOCK: bucket B Orders accessor --------------------------------------------------
    // --- APPEND BLOCK: bucket C Shipping & Delivery accessor -------------------------------------
    // --- APPEND BLOCK: bucket D Dictionaries accessor --------------------------------------------
    /** Access to the marketplace reference dictionaries ({@code /dictionaries}). */
    public DictionaryAccess dictionaries() {
        ensureOpen();
        return dictionaries;
    }


    // --- APPEND BLOCK: bucket E Finance accessor -------------------------------------------------
    // --- APPEND BLOCK: bucket F Comms & Automation accessor --------------------------------------

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("ErliClient has been closed");
        }
    }

    @Override
    public void close() {
        closed.set(true);
    }

    /** Builder for {@link ErliClient}. Base URL and API key are required; everything else has a default. */
    public static final class Builder {

        private String baseUrl;
        private ApiKey apiKey;
        private RetryPolicy retryPolicy = RetryPolicy.defaultPolicy();
        private String userAgent = DEFAULT_USER_AGENT;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
        private HttpClient httpClient;

        private Builder() {
        }

        public Builder baseUrl(String value) {
            this.baseUrl = value;
            return this;
        }

        /** Read the base URL from the {@value #BASE_URL_ENV_VAR} environment variable. */
        public Builder baseUrlFromEnvironment() {
            String value = System.getenv(BASE_URL_ENV_VAR);
            if (value == null || value.isBlank()) {
                throw new ErliConfigurationException(
                        "Environment variable " + BASE_URL_ENV_VAR + " is not set or is blank");
            }
            return baseUrl(value);
        }

        public Builder apiKey(ApiKey value) {
            this.apiKey = value;
            return this;
        }

        public Builder retryPolicy(RetryPolicy value) {
            this.retryPolicy = Objects.requireNonNull(value, "retryPolicy");
            return this;
        }

        public Builder userAgent(String value) {
            this.userAgent = Objects.requireNonNull(value, "userAgent");
            return this;
        }

        public Builder connectTimeout(Duration value) {
            this.connectTimeout = Objects.requireNonNull(value, "connectTimeout");
            return this;
        }

        public Builder requestTimeout(Duration value) {
            this.requestTimeout = Objects.requireNonNull(value, "requestTimeout");
            return this;
        }

        /** Supply a preconfigured {@link HttpClient} (e.g. with a proxy); otherwise one is created. */
        public Builder httpClient(HttpClient value) {
            this.httpClient = value;
            return this;
        }

        public ErliClient build() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new ErliConfigurationException("baseUrl is required");
            }
            if (apiKey == null) {
                throw new ErliConfigurationException("apiKey is required");
            }
            return new ErliClient(this);
        }
    }
}
