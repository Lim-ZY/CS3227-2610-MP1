package timey.ports;

import java.time.Instant;

/** Schedules a one-off local action for a future instant. */
@FunctionalInterface
public interface ReminderScheduler {
    ReminderHandle schedule(Instant triggerAt, Runnable action);
}
