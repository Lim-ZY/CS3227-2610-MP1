package Timey.infrastructure.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ScheduledExecutorReminderSchedulerTest {
    @Test
    void schedule_executorRunsEarly_reschedulesUntilTriggerTime() {
        var clock = new MutableClock(Instant.parse("2026-08-21T09:00:00Z"), ZoneId.of("Asia/Singapore"));
        var executor = new CapturingScheduledExecutor();
        var scheduler = new ScheduledExecutorReminderScheduler(executor, clock);
        var notifications = new AtomicInteger();
        Instant triggerAt = Instant.parse("2026-08-21T09:00:01.500Z");

        scheduler.schedule(triggerAt, notifications::incrementAndGet);
        assertEquals(Duration.ofMillis(1500).toNanos(), executor.delay);
        assertEquals(TimeUnit.NANOSECONDS, executor.unit);

        executor.runScheduledAction();
        assertEquals(0, notifications.get());

        clock.setInstant(triggerAt);
        executor.runScheduledAction();
        assertEquals(1, notifications.get());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }
    }

    private static final class CapturingScheduledExecutor extends AbstractExecutorService
            implements ScheduledExecutorService {
        private Runnable scheduledAction;
        private long delay;
        private TimeUnit unit;
        private boolean shutdown;

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            this.scheduledAction = command;
            this.delay = delay;
            this.unit = unit;
            return new CompletedScheduledFuture<>();
        }

        private void runScheduledAction() {
            Runnable action = scheduledAction;
            scheduledAction = null;
            action.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CompletedScheduledFuture<V> implements ScheduledFuture<V> {
        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public V get() {
            return null;
        }

        @Override
        public V get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}
