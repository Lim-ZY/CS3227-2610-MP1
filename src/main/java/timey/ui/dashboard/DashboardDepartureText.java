package timey.ui.dashboard;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/** Formats the remaining time before a locally scheduled departure. */
public final class DashboardDepartureText {
    private DashboardDepartureText() {
    }

    /** Returns the remaining duration before a leave-by datetime. */
    public static String until(LocalDateTime departureAt, Clock clock) {
        Duration remaining = Duration.between(LocalDateTime.now(clock), departureAt);
        if (remaining.isZero() || remaining.isNegative()) {
            return "Leave now";
        }
        if (remaining.compareTo(Duration.ofMinutes(1)) < 0) {
            return "<1m";
        }
        long hours = remaining.toHours();
        long minutes = remaining.toMinutesPart();
        return hours == 0 ? minutes + "m" : hours + "h " + minutes + "m";
    }
}
