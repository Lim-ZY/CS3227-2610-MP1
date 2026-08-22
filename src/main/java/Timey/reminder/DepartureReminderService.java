package Timey.reminder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.alert.ScheduledDepartureReminder;
import Timey.ports.ReminderHandle;
import Timey.ports.ReminderScheduler;

/** Schedules a physical-event reminder at the selected route's leave-by time. */
public final class DepartureReminderService {
    private final ReminderScheduler reminderScheduler;
    private final Clock clock;
    private final List<ScheduledDepartureReminder> scheduledReminders = new ArrayList<>();
    private final Map<ScheduledDepartureReminder, ReminderHandle> reminderHandles = new HashMap<>();

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
        ReminderHandle handle = reminderScheduler.schedule(reminder.triggerAt(), () -> {
            try {
                notification.run();
            } finally {
                removeAfterNotification(reminder);
            }
        });
        reminderHandles.put(reminder, handle);
        return reminder;
    }

    /** Returns currently active reminders, discarding entries whose trigger time has passed. */
    public synchronized List<ScheduledDepartureReminder> scheduledReminders() {
        discardPastReminders(Instant.now(clock));
        return List.copyOf(scheduledReminders);
    }

    /** Cancels the one-based active reminder number, if it exists. */
    public synchronized boolean cancel(int reminderNumber) {
        discardPastReminders(Instant.now(clock));
        if (reminderNumber < 1 || reminderNumber > scheduledReminders.size()) {
            return false;
        }
        cancelReminder(scheduledReminders.get(reminderNumber - 1));
        return true;
    }

    private synchronized void removeAfterNotification(ScheduledDepartureReminder reminder) {
        scheduledReminders.remove(reminder);
        reminderHandles.remove(reminder);
    }

    private void discardPastReminders(Instant now) {
        List<ScheduledDepartureReminder> pastReminders = scheduledReminders.stream()
                .filter(reminder -> reminder.triggerAt().isBefore(now))
                .toList();
        pastReminders.forEach(this::cancelReminder);
    }

    private void cancelReminder(ScheduledDepartureReminder reminder) {
        scheduledReminders.remove(reminder);
        ReminderHandle handle = reminderHandles.remove(reminder);
        if (handle != null) {
            handle.cancel();
        }
    }
}
