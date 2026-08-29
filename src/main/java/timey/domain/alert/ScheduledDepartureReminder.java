package timey.domain.alert;

import java.time.Instant;
import java.util.Objects;

/** A departure notification scheduled for a specific instant. */
public record ScheduledDepartureReminder(Instant triggerAt, String message) {
    /** Performs this operation. */
    public ScheduledDepartureReminder {
        triggerAt = Objects.requireNonNull(triggerAt);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Reminder message must not be blank.");
        }
    }
}
