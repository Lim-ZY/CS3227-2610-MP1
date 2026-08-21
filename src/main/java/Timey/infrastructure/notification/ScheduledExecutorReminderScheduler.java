package Timey.infrastructure.notification;

import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import Timey.ports.ReminderHandle;
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
    public ReminderHandle schedule(Instant triggerAt, Runnable action) {
        Objects.requireNonNull(triggerAt);
        Objects.requireNonNull(action);
        var cancelled = new AtomicBoolean();
        var scheduledTask = new AtomicReference<ScheduledFuture<?>>();
        scheduleAction(triggerAt, action, cancelled, scheduledTask);
        return () -> {
            cancelled.set(true);
            var task = scheduledTask.get();
            if (task != null) {
                task.cancel(false);
            }
        };
    }

    private void scheduleAction(Instant triggerAt, Runnable action, AtomicBoolean cancelled,
            AtomicReference<ScheduledFuture<?>> scheduledTask) {
        Duration delay = Duration.between(Instant.now(clock), triggerAt);
        scheduledTask.set(executor.schedule(() -> runWhenDue(triggerAt, action, cancelled, scheduledTask),
                nonNegativeNanos(delay), TimeUnit.NANOSECONDS));
    }

    private void runWhenDue(Instant triggerAt, Runnable action, AtomicBoolean cancelled,
            AtomicReference<ScheduledFuture<?>> scheduledTask) {
        if (cancelled.get()) {
            return;
        }
        if (Instant.now(clock).isBefore(triggerAt)) {
            scheduleAction(triggerAt, action, cancelled, scheduledTask);
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
