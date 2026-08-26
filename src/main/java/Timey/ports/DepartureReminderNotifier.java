package Timey.ports;

import Timey.domain.alert.ScheduledDepartureReminder;

/** Delivers a departure reminder when its scheduled time is reached. */
@FunctionalInterface
public interface DepartureReminderNotifier {
    void notifyDeparture(ScheduledDepartureReminder reminder);
}
