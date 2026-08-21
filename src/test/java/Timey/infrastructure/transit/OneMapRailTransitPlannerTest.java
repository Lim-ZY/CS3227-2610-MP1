package Timey.infrastructure.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import Timey.domain.location.ResolvedLocation;
import Timey.infrastructure.http.HttpResult;

class OneMapRailTransitPlannerTest {
    private static final ResolvedLocation COM3 = new ResolvedLocation("COM3", "COM3", 1.294, 103.773);
    private static final ResolvedLocation VIVOCITY = new ResolvedLocation("VivoCity", "VivoCity", 1.264, 103.822);

    @Test
    void requestsRailPublicTransportAndMapsReturnedItineraries() {
        var planner = new OneMapRailTransitPlanner((uri, authorization) -> {
            assertTrue(uri.toString().contains("routeType=pt&mode=rail"));
            assertTrue(uri.toString().contains("numItineraries=3"));
            assertEquals("access-token", authorization);
            return new HttpResult(200, """
                    {"plan":{"itineraries":[{"duration":2400,"walkTime":600,"transitTime":1800,"transfers":1},
                    {"duration":2700,"walkTime":900,"transitTime":1800,"transfers":0}]}}""");
        }, Optional.of("access-token"));

        var routes = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.of(9, 30));

        assertEquals(2, routes.size());
        assertEquals("Live rail route 1", routes.getFirst().name());
        assertEquals(40, routes.getFirst().totalDuration().toMinutes());
        assertEquals(1, routes.getFirst().transferCount());
    }

    @Test
    void returnsNoRoutesWithoutAToken() {
        var planner = new OneMapRailTransitPlanner((uri, authorization) -> {
            throw new AssertionError("No request should be made without a token.");
        }, Optional.empty());

        assertTrue(planner.findRoutes(COM3, VIVOCITY, LocalDate.now(), LocalTime.NOON).isEmpty());
    }
}
