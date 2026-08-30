package timey.infrastructure.http;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/** Retries one rate-limited HTTP response after the required waiting period. */
public final class RateLimitedHttpRequester implements HttpRequester {
    private static final Duration RATE_LIMIT_RETRY_DELAY = Duration.ofSeconds(10);

    private final HttpRequester delegate;
    private final RetryDelay retryDelay;

    /** Creates a requester that waits ten seconds before its one rate-limit retry. */
    public RateLimitedHttpRequester(HttpRequester delegate) {
        this(delegate, duration -> Thread.sleep(duration));
    }

    RateLimitedHttpRequester(HttpRequester delegate, RetryDelay retryDelay) {
        this.delegate = Objects.requireNonNull(delegate);
        this.retryDelay = Objects.requireNonNull(retryDelay);
    }

    @Override
    public HttpResult get(URI uri) {
        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException("HTTP request was interrupted.");
        }
        HttpResult firstResult = delegate.get(uri);
        if (firstResult.statusCode() != 429) {
            return firstResult;
        }
        pauseBeforeRetry();
        return delegate.get(uri);
    }

    private void pauseBeforeRetry() {
        try {
            retryDelay.pause(RATE_LIMIT_RETRY_DELAY);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP retry was interrupted.", exception);
        }
    }
}
