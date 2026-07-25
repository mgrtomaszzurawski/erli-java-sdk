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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The single transport chokepoint: builds requests against the configured base URL, injects the
 * Bearer credential and standard headers, executes them on {@link java.net.http.HttpClient}, applies
 * the {@link RetryPolicy} (429/5xx and transport failures, honoring {@code Retry-After} as a floor),
 * and maps non-2xx responses to the remediation exceptions via {@link ErrorMapper}. Exposes the full
 * verb surface the buckets need — GET/POST/PUT/PATCH/DELETE, object and bare-array responses, and
 * URL-encoded {@link QueryParameters}. Retry idempotency follows HTTP semantics: GET/PUT/DELETE are
 * retryable; POST/PATCH are not (unless {@code retryPost} is set). Built once by {@code ErliClient}.
 * Internal: never exported.
 */
public final class HttpRuntime {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String MEDIA_TYPE_JSON = "application/json";

    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_PATCH = "PATCH";
    private static final String METHOD_DELETE = "DELETE";

    private static final boolean IDEMPOTENT = true;
    private static final boolean NON_IDEMPOTENT = false;

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

    /** {@code GET path} with no query parameters, decoding a JSON object into {@code responseType}. */
    public <T> T get(String path, Class<T> responseType) {
        return get(path, QueryParameters.empty(), responseType);
    }

    /** {@code GET path} with URL-encoded query parameters, decoding a JSON object. */
    public <T> T get(String path, QueryParameters queryParameters, Class<T> responseType) {
        HttpRequest request = queryRequest(path, queryParameters).GET().build();
        return execute(request, path, IDEMPOTENT, body -> decodeObject(body, responseType));
    }

    /** {@code GET path} with query parameters, decoding a bare JSON array into a {@code List}. */
    public <T> List<T> getList(String path, QueryParameters queryParameters, Class<T> elementType) {
        HttpRequest request = queryRequest(path, queryParameters).GET().build();
        return execute(request, path, IDEMPOTENT, body -> decodeList(body, elementType));
    }

    /** {@code POST path} with a JSON body, decoding a JSON object into {@code responseType}. */
    public <T> T post(String path, Object requestBody, Class<T> responseType) {
        return execute(bodyRequest(METHOD_POST, path, requestBody), path, NON_IDEMPOTENT,
                body -> decodeObject(body, responseType));
    }

    /** {@code POST path} with a JSON body, decoding a bare JSON array into a {@code List}. */
    public <T> List<T> postList(String path, Object requestBody, Class<T> elementType) {
        return execute(bodyRequest(METHOD_POST, path, requestBody), path, NON_IDEMPOTENT,
                body -> decodeList(body, elementType));
    }

    /** {@code PUT path} with a JSON body. PUT is idempotent, so it participates in retries. */
    public <T> T put(String path, Object requestBody, Class<T> responseType) {
        return execute(bodyRequest(METHOD_PUT, path, requestBody), path, IDEMPOTENT,
                body -> decodeObject(body, responseType));
    }

    /** {@code PATCH path} with a JSON body. PATCH is not idempotent, so it is not retried by default. */
    public <T> T patch(String path, Object requestBody, Class<T> responseType) {
        return execute(bodyRequest(METHOD_PATCH, path, requestBody), path, NON_IDEMPOTENT,
                body -> decodeObject(body, responseType));
    }

    /**
     * {@code DELETE path} with query parameters, decoding a JSON object (or {@code null} when the
     * response is empty, e.g. HTTP 204). Use {@code Void.class} when no body is expected.
     */
    public <T> T delete(String path, QueryParameters queryParameters, Class<T> responseType) {
        HttpRequest request = queryRequest(path, queryParameters).DELETE().build();
        return execute(request, path, IDEMPOTENT, body -> decodeObject(body, responseType));
    }

    /**
     * {@code DELETE path} with a JSON request body — a few Erli deletes take one (e.g.
     * {@code DELETE /dictionaries/attachments} with an id array). DELETE is idempotent, so it is
     * retried; an empty response decodes to {@code null} (use {@code Void.class}).
     */
    public <T> T delete(String path, Object requestBody, QueryParameters queryParameters, Class<T> responseType) {
        HttpRequest.BodyPublisher publisher =
                HttpRequest.BodyPublishers.ofString(codec.write(requestBody), StandardCharsets.UTF_8);
        HttpRequest request = queryRequest(path, queryParameters)
                .header(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
                .method(METHOD_DELETE, publisher)
                .build();
        return execute(request, path, IDEMPOTENT, body -> decodeObject(body, responseType));
    }

    private HttpRequest.Builder queryRequest(String path, QueryParameters queryParameters) {
        return baseRequest(queryParameters.appendTo(baseUrl + path)).header(HEADER_ACCEPT, MEDIA_TYPE_JSON);
    }

    private HttpRequest bodyRequest(String method, String path, Object requestBody) {
        HttpRequest.BodyPublisher publisher =
                HttpRequest.BodyPublishers.ofString(codec.write(requestBody), StandardCharsets.UTF_8);
        // Builder.method(name, publisher) is the general form of .POST()/.PUT() and also carries PATCH.
        return baseRequest(baseUrl + path)
                .header(HEADER_ACCEPT, MEDIA_TYPE_JSON)
                .header(HEADER_CONTENT_TYPE, MEDIA_TYPE_JSON)
                .method(method, publisher)
                .build();
    }

    private HttpRequest.Builder baseRequest(String uri) {
        return HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(requestTimeout)
                .header(HEADER_AUTHORIZATION, apiKey.authorizationHeaderValue())
                .header(HEADER_USER_AGENT, userAgent);
    }

    private <T> T execute(HttpRequest request, String path, boolean idempotent, Function<String, T> decoder) {
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
                return decoder.apply(response.body());
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

    private <T> T decodeObject(String body, Class<T> responseType) {
        if (responseType == Void.class || body == null || body.isBlank()) {
            return null;
        }
        return codec.read(body, responseType);
    }

    private <T> List<T> decodeList(String body, Class<T> elementType) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        return codec.readList(body, elementType);
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
            // HTTP-date form is not honored as a floor here; fall back to computed backoff.
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
