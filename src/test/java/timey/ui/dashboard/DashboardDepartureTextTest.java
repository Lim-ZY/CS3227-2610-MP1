package timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DashboardDepartureTextTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T01:42:00Z"),
            ZoneId.of("Asia/Singapore"));

    @Test
    void until_futureDeparture_formatsHoursAndMinutes() {
        assertEquals("7h 55m", DashboardDepartureText.until(LocalDateTime.of(2026, 8, 21, 17, 37), CLOCK));
    }

    @Test
    void until_departureReached_showsLeaveNow() {
        assertEquals("Leave now", DashboardDepartureText.until(LocalDateTime.of(2026, 8, 21, 9, 42), CLOCK));
    }

    @Test
    void until_departureWithinOneMinute_showsLessThanOneMinute() {
        assertEquals("<1m", DashboardDepartureText.until(LocalDateTime.of(2026, 8, 21, 9, 42, 30), CLOCK));
    }

    @Test
    void until_tomorrowDeparture_includesRemainingDay() {
        assertEquals("22h 18m", DashboardDepartureText.until(LocalDateTime.of(2026, 8, 22, 8, 0), CLOCK));
    }
}
