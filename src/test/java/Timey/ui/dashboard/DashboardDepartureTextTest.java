package Timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DashboardDepartureTextTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T01:42:00Z"),
            ZoneId.of("Asia/Singapore"));

    @Test
    void until_futureDeparture_formatsHoursAndMinutes() {
        assertEquals("7h 55m", DashboardDepartureText.until(LocalTime.of(17, 37), CLOCK));
    }

    @Test
    void until_departureReached_showsLeaveNow() {
        assertEquals("Leave now", DashboardDepartureText.until(LocalTime.of(9, 42), CLOCK));
    }
}
