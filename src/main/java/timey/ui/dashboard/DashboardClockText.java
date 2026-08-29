package timey.ui.dashboard;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats the current dashboard clock using the configured local time zone. */
public final class DashboardClockText {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMM · HH:mm", Locale.ENGLISH);

    private DashboardClockText() {
    }

    public static String now(Clock clock) {
        return FORMAT.format(clock.instant().atZone(clock.getZone()));
    }
}
