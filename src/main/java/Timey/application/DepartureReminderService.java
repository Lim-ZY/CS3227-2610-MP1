package Timey.application;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.alert.ScheduledDepartureReminder;
import Timey.ports.ReminderScheduler;

/** Schedules a physical-event reminder at the selected route's leave-by time. */
public final class DepartureReminderService {
    private final ReminderScheduler reminderScheduler;
    private final Clock clock;

    public DepartureReminderService(ReminderScheduler reminderScheduler, Clock clock) {
        this.reminderScheduler = Objects.requireNonNull(reminderScheduler);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Schedules a reminder today, or tomorrow when today's leave-by time has passed. */
    public ScheduledDepartureReminder schedule(DepartureRecommendation recommendation, Runnable notification) {
        Objects.requireNonNull(recommendation);
        Objects.requireNonNull(notification);
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime triggerAt = now.with(recommendation.departureTime());
        if (!triggerAt.isAfter(now)) {
            triggerAt = triggerAt.plusDays(1);
        }
        String message = "Timey reminder: Please leave your desk now.";
        reminderScheduler.schedule(triggerAt.toInstant(), notification);
        return new ScheduledDepartureReminder(triggerAt.toInstant(), message);
    }
}
