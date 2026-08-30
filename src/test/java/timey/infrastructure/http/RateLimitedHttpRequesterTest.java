package timey.infrastructure.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RateLimitedHttpRequesterTest {
    private static final URI URI = java.net.URI.create("https://example.test/routes");

    @Test
    void get_rateLimitThenSuccess_retriesOnce() {
        var calls = new AtomicInteger();
        List<Duration> delays = new ArrayList<>();
        var requester = new RateLimitedHttpRequester(uri -> switch (calls.getAndIncrement()) {
            case 0 -> new HttpResult(429, "rate limited");
            default -> new HttpResult(200, "ok");
        }, delays::add);

        var result = requester.get(URI);

        assertEquals(200, result.statusCode());
        assertEquals(2, calls.get());
        assertEquals(List.of(Duration.ofSeconds(10)), delays);
    }

    @Test
    void get_secondRateLimit_returnsItWithoutAnotherRetry() {
        var calls = new AtomicInteger();
        List<Duration> delays = new ArrayList<>();
        var requester = new RateLimitedHttpRequester(uri -> {
            calls.incrementAndGet();
            return new HttpResult(429, "rate limited");
        }, delays::add);

        var result = requester.get(URI);

        assertEquals(429, result.statusCode());
        assertEquals(2, calls.get());
        assertEquals(List.of(Duration.ofSeconds(10)), delays);
    }

    @Test
    void get_nonRateLimitedResponse_doesNotRetry() {
        var calls = new AtomicInteger();
        var requester = new RateLimitedHttpRequester(uri -> {
            calls.incrementAndGet();
            return new HttpResult(503, "unavailable");
        }, duration -> {
            throw new AssertionError("Only rate limits should pause.");
        });

        var result = requester.get(URI);

        assertEquals(503, result.statusCode());
        assertEquals(1, calls.get());
    }

    @Test
    void get_interruptedBeforeRequest_doesNotInvokeDelegate() {
        var calls = new AtomicInteger();
        var requester = new RateLimitedHttpRequester(uri -> {
            calls.incrementAndGet();
            return new HttpResult(200, "ok");
        }, duration -> { });

        Thread.currentThread().interrupt();
        try {
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> requester.get(URI));

            assertEquals("HTTP request was interrupted.", exception.getMessage());
            assertEquals(0, calls.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
