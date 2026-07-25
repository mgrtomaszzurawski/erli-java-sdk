package io.github.mgrtomaszzurawski.erli.internal;

import io.github.mgrtomaszzurawski.erli.core.auth.ApiKey;
import io.github.mgrtomaszzurawski.erli.core.error.ErliTransportException;
import io.github.mgrtomaszzurawski.erli.core.retry.RetryPolicy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * The single transport chokepoint: builds requests against the configured base URL, injects the
 * Bearer credential and standard headers, executes them on {@link java.net.http.HttpClient}, applies
 * the {@link RetryPolicy} (429/5xx and transport failures, honoring {@code Retry-After} as a floor),
 * and maps non-2xx responses to the remediation exceptions via {@link ErrorMapper}. Every domain
 * bucket goes through here — it is built once by {@code ErliClient}. Internal: never exported.
 */
public final class HttpRuntime {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String MEDIA_TYPE_JSON = "application/json";

    private static final int HTTP_OK_MIN = 200;
    private static final int HTTP_OK_MAX_EXCLUSIVE = 300;
    private static final int FIRST_RETRY_INDEX = 0;

    private final HttpClient httpClient;
    private final String baseUrl;
    private final ApiKey apiKey;
    private final RetryPolicy retryPolicy;
    private final String userAgent;
    private final Duration requestTimeout;
    private final JsonCodec codec;
    private final ErrorMapper errorMapper;

    public HttpRuntime(HttpClient httpClient, String baseUrl, ApiKey apiKey, RetryPolicy retryPolicy,
            String userAgent, Duration requestTimeout, JsonCodec codec, ErrorMapper errorMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.baseUrl = stripTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl"));
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.errorMapper = Objects.requireNonNull(errorMapper, "errorMapper");
    }

    /** Execute {@code GET path} and decode the JSON body into {@code responseType}. */
    public <T> T get(String path, Class<T> responseType) {
        HttpRequest request = baseRequest(path)
                .header(HEADER_ACCEPT, MEDIA_TYPE_JSON)
                .GET()
                .build();
        return execute(request, path, responseType, true);
    }

    /** Execute {@code POST path} with a JSON-serialized {@code requestBody}, decoding into {@code responseType}. */
    public <T> T post(String path, Object requestBody, Class<T> responseType) {
        HttpRequest request = baseRequest(path)
                .header(HEADER_ACCEPT, MEDIA_TYPE_JSON)
                .header(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(codec.write(requestBody), StandardCharsets.UTF_8))
                .build();
        return execute(request, path, responseType, false);
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header(HEADER_AUTHORIZATION, apiKey.authorizationHeaderValue())
                .header(HEADER_USER_AGENT, userAgent);
    }

    private <T> T execute(HttpRequest request, String path, Class<T> responseType, boolean idempotent) {
        int attempt = 0;
        int retryIndex = FIRST_RETRY_INDEX;
        while (true) {
            attempt++;
            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException failure) {
                if (canRetry(attempt) && retryPolicy.isRetryableTransportFailure(idempotent)) {
                    sleepBackoff(retryIndex++, null);
                    continue;
                }
                throw new ErliTransportException("Request to " + path + " failed", failure);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new ErliTransportException("Request to " + path + " was interrupted", interrupted);
            }

            int status = response.statusCode();
            if (isSuccess(status)) {
                return decode(response.body(), responseType);
            }
            if (canRetry(attempt) && retryPolicy.isRetryableStatus(status, idempotent)) {
                sleepBackoff(retryIndex++, retryAfterFloor(response));
                continue;
            }
            throw errorMapper.toException(status, response.body());
        }
    }

    private boolean canRetry(int attempt) {
        return attempt < retryPolicy.maxAttempts();
    }

    private <T> T decode(String body, Class<T> responseType) {
        if (responseType == Void.class || body == null || body.isBlank()) {
            return null;
        }
        return codec.read(body, responseType);
    }

    private void sleepBackoff(int retryIndex, Duration retryAfterFloor) {
        Duration wait = retryPolicy.backoff(retryIndex, retryAfterFloor);
        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ErliTransportException("Interrupted while backing off before a retry", interrupted);
        }
    }

    private Duration retryAfterFloor(HttpResponse<String> response) {
        Optional<String> header = response.headers().firstValue(HEADER_RETRY_AFTER);
        if (header.isEmpty()) {
            return null;
        }
        try {
            long seconds = Long.parseLong(header.get().trim());
            return seconds >= 0 ? Duration.ofSeconds(seconds) : null;
        } catch (NumberFormatException notAnInteger) {
            // HTTP-date form is not honored as a floor in M1; fall back to computed backoff.
            return null;
        }
    }

    private static boolean isSuccess(int status) {
        return status >= HTTP_OK_MIN && status < HTTP_OK_MAX_EXCLUSIVE;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
