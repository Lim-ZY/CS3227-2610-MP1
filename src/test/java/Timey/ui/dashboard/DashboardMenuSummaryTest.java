package Timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import Timey.parser.PlanCommand;
import Timey.ui.DashboardState;

class DashboardMenuSummaryTest {
    @Test
    void summaries_planAvailable_showsRecentLocationsAndBuffer() {
        DashboardState state = new DashboardState(Optional.of(
                new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ofMinutes(10))),
                List.of(), List.of(), Optional.empty(), List.of());

        assertEquals("COM3 → VivoCity", DashboardMenuSummary.recentLocations(state));
        assertEquals("10 minutes (current plan)", DashboardMenuSummary.personalBuffer(state));
    }

    @Test
    void summaries_noPlan_showsEmptyState() {
        DashboardState state = new DashboardState(Optional.empty(), List.of(), List.of(), Optional.empty(), List.of());

        assertEquals("No recent plan", DashboardMenuSummary.recentLocations(state));
        assertEquals("Set per plan", DashboardMenuSummary.personalBuffer(state));
    }
}
