package Timey.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import Timey.domain.alert.DepartureRecommendation;
import Timey.ports.ReminderHandle;
import Timey.ports.ReminderScheduler;

class DepartureReminderServiceTest {
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    @Test
    void schedule_departureLaterToday_schedulesReminderToday() {
        var scheduler = new CapturingReminderScheduler();
        var service = new DepartureReminderService(scheduler,
                Clock.fixed(Instant.parse("2026-08-21T08:00:00Z"), SINGAPORE));
        var recommendation = new DepartureRecommendation("Rail", LocalTime.of(17, 30), Duration.ofMinutes(43),
                Duration.ofMinutes(10));

        var reminder = service.schedule(recommendation, () -> { });

        assertEquals(Instant.parse("2026-08-21T09:30:00Z"), reminder.triggerAt());
        assertEquals(reminder.triggerAt(), scheduler.triggerAt.get());
        assertEquals("Timey reminder: Please leave your desk now.", reminder.message());
    }

    @Test
    void schedule_departureTimeNowOrPast_schedulesReminderTomorrow() {
        var scheduler = new CapturingReminderScheduler();
        var service = new DepartureReminderService(scheduler,
                Clock.fixed(Instant.parse("2026-08-21T09:30:00Z"), SINGAPORE));
        var recommendation = new DepartureRecommendation("Rail", LocalTime.of(17, 30), Duration.ofMinutes(43),
                Duration.ofMinutes(10));

        var reminder = service.schedule(recommendation, () -> { });

        assertEquals(Instant.parse("2026-08-22T09:30:00Z"), reminder.triggerAt());
    }

    @Test
    void scheduledReminders_triggerTimePassed_discardsReminder() {
        var clock = new MutableClock(Instant.parse("2026-08-21T08:00:00Z"), SINGAPORE);
        var scheduler = new CapturingReminderScheduler();
        var service = new DepartureReminderService(scheduler, clock);
        var recommendation = new DepartureRecommendation("Rail", LocalTime.of(17, 30), Duration.ofMinutes(43),
                Duration.ofMinutes(10));

        service.schedule(recommendation, () -> { });
        clock.setInstant(Instant.parse("2026-08-21T09:30:01Z"));

        assertEquals(java.util.List.of(), service.scheduledReminders());
        assertEquals(true, scheduler.cancelled.get());
    }

    @Test
    void cancel_activeReminder_removesReminderAndCancelsScheduledAction() {
        var scheduler = new CapturingReminderScheduler();
        var service = new DepartureReminderService(scheduler,
                Clock.fixed(Instant.parse("2026-08-21T08:00:00Z"), SINGAPORE));
        var recommendation = new DepartureRecommendation("Rail", LocalTime.of(17, 30), Duration.ofMinutes(43),
                Duration.ofMinutes(10));

        service.schedule(recommendation, () -> { });

        assertEquals(true, service.cancel(1));
        assertEquals(java.util.List.of(), service.scheduledReminders());
        assertEquals(true, scheduler.cancelled.get());
    }

    private static final class CapturingReminderScheduler implements ReminderScheduler {
        private final AtomicReference<Instant> triggerAt = new AtomicReference<>();
        private final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();

        @Override
        public ReminderHandle schedule(Instant triggerAt, Runnable action) {
            this.triggerAt.set(triggerAt);
            return () -> cancelled.set(true);
        }
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
}
