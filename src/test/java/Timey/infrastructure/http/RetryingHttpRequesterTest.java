package Timey.infrastructure.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RetryingHttpRequesterTest {
    private static final URI URI = java.net.URI.create("https://example.test/routes");

    @Test
    void constructor_negativeRetryLimit_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new RetryingHttpRequester((uri, authorization) -> new HttpResult(200, "ok"), -1,
                        duration -> { }));

        assertEquals("Maximum retries must not be negative.", exception.getMessage());
    }

    @Test
    void get_transientHttpFailures_thenSuccessRetriesAndReturnsSuccess() {
        var calls = new AtomicInteger();
        List<Duration> delays = new ArrayList<>();
        HttpRequester requester = (uri, authorization) -> switch (calls.getAndIncrement()) {
        case 0 -> new HttpResult(429, "rate limited");
        case 1 -> new HttpResult(503, "unavailable");
        default -> new HttpResult(200, "ok");
        };
        var retryingRequester = new RetryingHttpRequester(requester, 2, delays::add);

        var result = retryingRequester.get(URI, "token");

        assertEquals(200, result.statusCode());
        assertEquals(3, calls.get());
        assertEquals(List.of(Duration.ofMillis(100), Duration.ofMillis(200)), delays);
    }

    @Test
    void get_permanentClientError_returnsWithoutRetrying() {
        var calls = new AtomicInteger();
        var retryingRequester = new RetryingHttpRequester((uri, authorization) -> {
            calls.incrementAndGet();
            return new HttpResult(400, "bad request");
        }, 2, duration -> {
            throw new AssertionError("Permanent failures should not pause.");
        });

        var result = retryingRequester.get(URI, "token");

        assertEquals(400, result.statusCode());
        assertEquals(1, calls.get());
    }

    @Test
    void get_connectionFailure_retriesOnlyToConfiguredLimit() {
        var calls = new AtomicInteger();
        var retryingRequester = new RetryingHttpRequester((uri, authorization) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("Timed out");
        }, 2, duration -> { });

        assertThrows(IllegalStateException.class, () -> retryingRequester.get(URI, "token"));
        assertEquals(3, calls.get());
    }

    @Test
    void get_persistentServerError_returnsFinalServerResponse() {
        var calls = new AtomicInteger();
        var retryingRequester = new RetryingHttpRequester((uri, authorization) -> {
            calls.incrementAndGet();
            return new HttpResult(503, "unavailable");
        }, 2, duration -> { });

        var result = retryingRequester.get(URI, "token");

        assertEquals(503, result.statusCode());
        assertEquals(3, calls.get());
    }
}
