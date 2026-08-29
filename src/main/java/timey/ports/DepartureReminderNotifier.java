package timey.ports;

import timey.domain.alert.ScheduledDepartureReminder;

/** Delivers a departure reminder when its scheduled time is reached. */
@FunctionalInterface
public interface DepartureReminderNotifier {
    void notifyDeparture(ScheduledDepartureReminder reminder);
}
