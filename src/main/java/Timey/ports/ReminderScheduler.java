package Timey.ports;

import java.time.Instant;

/** Schedules a one-off local action for a future instant. */
@FunctionalInterface
public interface ReminderScheduler {
    void schedule(Instant triggerAt, Runnable action);
}
