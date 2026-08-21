package Timey.ports;

/** Cancels a previously scheduled local reminder action. */
@FunctionalInterface
public interface ReminderHandle {
    void cancel();
}
