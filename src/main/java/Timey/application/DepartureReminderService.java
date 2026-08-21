package Timey.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.alert.ScheduledDepartureReminder;
import Timey.ports.ReminderScheduler;

/** Schedules a physical-event reminder at the selected route's leave-by time. */
public final class DepartureReminderService {
    private final ReminderScheduler reminderScheduler;
    private final Clock clock;
    private final List<ScheduledDepartureReminder> scheduledReminders = new ArrayList<>();

    public DepartureReminderService(ReminderScheduler reminderScheduler, Clock clock) {
        this.reminderScheduler = Objects.requireNonNull(reminderScheduler);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Schedules a reminder today, or tomorrow when today's leave-by time has passed. */
    public synchronized ScheduledDepartureReminder schedule(DepartureRecommendation recommendation,
            Runnable notification) {
        Objects.requireNonNull(recommendation);
        Objects.requireNonNull(notification);
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime triggerAt = now.with(recommendation.departureTime());
        if (!triggerAt.isAfter(now)) {
            triggerAt = triggerAt.plusDays(1);
        }
        String message = "Timey reminder: Please leave your desk now.";
        ScheduledDepartureReminder reminder = new ScheduledDepartureReminder(triggerAt.toInstant(), message);
        scheduledReminders.add(reminder);
        reminderScheduler.schedule(reminder.triggerAt(), () -> {
            try {
                notification.run();
            } finally {
                remove(reminder);
            }
        });
        return reminder;
    }

    /** Returns currently active reminders, discarding entries whose trigger time has passed. */
    public synchronized List<ScheduledDepartureReminder> scheduledReminders() {
        discardPastReminders(Instant.now(clock));
        return List.copyOf(scheduledReminders);
    }

    private synchronized void remove(ScheduledDepartureReminder reminder) {
        scheduledReminders.remove(reminder);
    }

    private void discardPastReminders(Instant now) {
        scheduledReminders.removeIf(reminder -> reminder.triggerAt().isBefore(now));
    }
}
