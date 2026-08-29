package timey.infrastructure.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class MockTransitPlannerTest {
    @Test
    void findRoutes_supportedLocations_returnsNoRoutes() {
        var planner = new MockTransitPlanner();

        List<?> routes = planner.findRoutes("COM3", "VivoCity");

        assertEquals(List.of(), routes);
    }
}
