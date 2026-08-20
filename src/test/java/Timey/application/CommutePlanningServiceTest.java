package Timey.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import Timey.command.PlanCommand;
import Timey.domain.transit.RouteAlternative;
import Timey.ports.TransitPlanner;

class CommutePlanningServiceTest {
    @Test
    void delegatesRouteLookupUsingPlanLocations() {
        RouteAlternative expectedRoute = new RouteAlternative(
                "Test route", Duration.ofMinutes(1), Duration.ofMinutes(2), 0);
        TransitPlanner transitPlanner = (origin, destination) -> {
            assertEquals("COM3", origin);
            assertEquals("VivoCity", destination);
            return List.of(expectedRoute);
        };
        var service = new CommutePlanningService(transitPlanner);
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ofMinutes(10));

        assertEquals(List.of(expectedRoute), service.findAlternatives(plan));
    }
}
