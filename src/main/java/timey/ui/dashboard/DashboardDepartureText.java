package timey.ui.dashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;

/** Formats the remaining time before a locally scheduled departure. */
public final class DashboardDepartureText {
    private DashboardDepartureText() {
    }

    /** Performs this operation. */
    public static String until(LocalTime departureTime, Clock clock) {
        Duration remaining = Duration.between(LocalTime.now(clock), departureTime);
        if (remaining.isZero() || remaining.isNegative()) {
            return "Leave now";
        }
        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();
        return hours == 0 ? minutes + "m" : hours + "h " + minutes + "m";
    }
}
