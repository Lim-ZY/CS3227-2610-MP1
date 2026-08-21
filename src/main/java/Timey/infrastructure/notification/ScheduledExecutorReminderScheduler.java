package Timey.infrastructure.notification;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import Timey.ports.ReminderScheduler;

/** Schedules local reminder actions on a daemon thread so the CLI can exit normally. */
public final class ScheduledExecutorReminderScheduler implements ReminderScheduler {
    private final ScheduledExecutorService executor;
    private final Clock clock;

    public ScheduledExecutorReminderScheduler() {
        this(Executors.newSingleThreadScheduledExecutor(daemonThreadFactory()), Clock.systemUTC());
    }

    ScheduledExecutorReminderScheduler(ScheduledExecutorService executor, Clock clock) {
        this.executor = Objects.requireNonNull(executor);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public void schedule(Instant triggerAt, Runnable action) {
        Objects.requireNonNull(triggerAt);
        Objects.requireNonNull(action);
        Duration delay = Duration.between(Instant.now(clock), triggerAt);
        executor.schedule(() -> runWhenDue(triggerAt, action), nonNegativeNanos(delay), TimeUnit.NANOSECONDS);
    }

    private void runWhenDue(Instant triggerAt, Runnable action) {
        if (Instant.now(clock).isBefore(triggerAt)) {
            schedule(triggerAt, action);
            return;
        }
        action.run();
    }

    private long nonNegativeNanos(Duration delay) {
        if (delay.isNegative() || delay.isZero()) {
            return 0;
        }
        try {
            return delay.toNanos();
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    private static ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "timey-reminders");
            thread.setDaemon(true);
            return thread;
        };
    }
}
