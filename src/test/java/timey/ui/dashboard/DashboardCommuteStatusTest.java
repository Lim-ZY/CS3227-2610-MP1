package timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import timey.domain.transit.RouteAlternative;
import timey.ui.DashboardState;

class DashboardCommuteStatusTest {
    private static final RouteAlternative ROUTE = new RouteAlternative("Public transport", Duration.ofMinutes(5),
            Duration.ofMinutes(25), 0);

    @Test
    void from_liveTransitResult_showsLiveStatus() {
        DashboardState state = new DashboardState(Optional.empty(), List.of(ROUTE),
                List.of("Live public transport routes were aligned with your target arrival time."), Optional.empty(),
                Optional.empty());

        assertEquals("Live public transport routes ready", DashboardCommuteStatus.from(state).title());
    }

    @Test
    void from_fallbackResult_showsFallbackStatus() {
        DashboardState state = new DashboardState(Optional.empty(), List.of(ROUTE),
                List.of("Using offline estimate: OneMap is unavailable."), Optional.empty(), Optional.empty());

        assertEquals("Using deterministic fallback", DashboardCommuteStatus.from(state).title());
    }

    @Test
    void from_noPlan_showsWaitingStatus() {
        DashboardState state = new DashboardState(Optional.empty(), List.of(), List.of(), Optional.empty(),
                Optional.empty());

        assertEquals("Waiting for a plan...", DashboardCommuteStatus.from(state).title());
        assertEquals("Live public transport alternatives will be requested once you execute `plan`.",
                DashboardCommuteStatus.from(state).message());
    }
}
