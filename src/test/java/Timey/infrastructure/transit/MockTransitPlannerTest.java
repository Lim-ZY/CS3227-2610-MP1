package Timey.infrastructure.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import Timey.domain.transit.RouteAlternative;

class MockTransitPlannerTest {
    @Test
    void returnsTwoStableRouteAlternatives() {
        var planner = new MockTransitPlanner();

        List<RouteAlternative> routes = planner.findRoutes("COM3", "VivoCity");

        assertEquals(List.of(
                new RouteAlternative("Fastest Transit", Duration.ofMinutes(8), Duration.ofMinutes(35), 1),
                new RouteAlternative("Direct Bus", Duration.ofMinutes(12), Duration.ofMinutes(47), 0)), routes);
    }
}
