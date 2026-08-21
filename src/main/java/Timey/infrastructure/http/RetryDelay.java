package Timey.infrastructure.http;

import java.time.Duration;

/** Waits between retry attempts, with an injectable implementation for deterministic tests. */
@FunctionalInterface
public interface RetryDelay {
    void pause(Duration duration) throws InterruptedException;
}
