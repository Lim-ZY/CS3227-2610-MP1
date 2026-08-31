package timey.domain.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class RouteAlternativeTest {
    @Test
    void totalDuration_walkingAndTransitLegs_returnsCombinedDuration() {
        RouteAlternative route = new RouteAlternative(
                "Fastest Transit", Duration.ofMinutes(8), Duration.ofMinutes(35), 1);

        assertEquals(Duration.ofMinutes(43), route.totalDuration());
    }

    @Test
    void steps_sourceListMutated_returnsImmutableRouteBreakdown() {
        var steps = new java.util.ArrayList<>(List.of(
                new RouteStep(RouteStepMode.WALK, "COM3", "Kent Ridge MRT", "walking", Duration.ofMinutes(6))));
        var route = new RouteAlternative("Public transport route", Duration.ofMinutes(6), Duration.ofMinutes(30),
                0, steps);
        steps.clear();

        assertEquals("Walk from COM3 to Kent Ridge MRT", route.steps().getFirst().description());
        assertEquals(1, route.steps().size());
    }

    @Test
    void routeStep_busMode_describesBusService() {
        var step = new RouteStep(RouteStepMode.BUS, "NUS Kent Ridge Terminal", "Clementi MRT", "95",
                Duration.ofMinutes(15));

        assertEquals("Take bus 95 from NUS Kent Ridge Terminal to Clementi MRT", step.description());
    }

    @Test
    void constructor_negativeDuration_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new RouteAlternative("Invalid", Duration.ofMinutes(-1), Duration.ZERO, 0));

        assertEquals("Walking duration must not be negative.", exception.getMessage());
    }
}
