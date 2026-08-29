package timey.infrastructure.http;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/** Retries transient HTTP failures with bounded exponential backoff. */
public final class RetryingHttpRequester implements HttpRequester {
    private static final int MAX_RETRIES = 2;
    private static final Duration INITIAL_DELAY = Duration.ofMillis(100);

    private final HttpRequester delegate;
    private final int maxRetries;
    private final RetryDelay retryDelay;

    public RetryingHttpRequester(HttpRequester delegate) {
        this(delegate, MAX_RETRIES, duration -> Thread.sleep(duration));
    }

    RetryingHttpRequester(HttpRequester delegate, int maxRetries, RetryDelay retryDelay) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Maximum retries must not be negative.");
        }
        this.delegate = Objects.requireNonNull(delegate);
        this.maxRetries = maxRetries;
        this.retryDelay = Objects.requireNonNull(retryDelay);
    }

    @Override
    public HttpResult get(URI uri) {
        IllegalStateException lastFailure = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpResult result = delegate.get(uri);
                if (!isTransient(result.statusCode()) || attempt == maxRetries) {
                    return result;
                }
            } catch (IllegalStateException exception) {
                lastFailure = exception;
                if (attempt == maxRetries) {
                    throw exception;
                }
            }
            pauseBeforeRetry(attempt);
        }
        throw lastFailure == null ? new IllegalStateException("HTTP request did not complete.") : lastFailure;
    }

    private boolean isTransient(int statusCode) {
        return statusCode == 429 || statusCode >= 500 && statusCode <= 599;
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            retryDelay.pause(INITIAL_DELAY.multipliedBy(1L << attempt));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP retry was interrupted.", exception);
        }
    }
}
