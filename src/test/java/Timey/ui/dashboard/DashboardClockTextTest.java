package Timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DashboardClockTextTest {
    @Test
    void now_singaporeClock_formatsDayDateAndTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-21T01:42:00Z"), ZoneId.of("Asia/Singapore"));

        assertEquals("Friday, 21 Aug · 09:42", DashboardClockText.now(clock));
    }
}
