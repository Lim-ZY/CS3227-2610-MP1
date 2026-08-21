package Timey.domain.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class RouteAlternativeTest {
    @Test
    void calculatesTotalDurationFromWalkingAndTransitLegs() {
        RouteAlternative route = new RouteAlternative(
                "Fastest Transit", Duration.ofMinutes(8), Duration.ofMinutes(35), 1);

        assertEquals(Duration.ofMinutes(43), route.totalDuration());
    }

    @Test
    void retainsAnImmutableItemisedRouteBreakdown() {
        var steps = new java.util.ArrayList<>(List.of(
                new RouteStep(RouteStepMode.WALK, "COM3", "Kent Ridge MRT", "walking", Duration.ofMinutes(6))));
        var route = new RouteAlternative("Rail route", Duration.ofMinutes(6), Duration.ofMinutes(30), 0, steps);
        steps.clear();

        assertEquals("Walk from COM3 to Kent Ridge MRT", route.steps().getFirst().description());
        assertEquals(1, route.steps().size());
    }
}
