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
    void findRoutes_validItineraries_mapsEveryItinerary() {
        var planner = new OneMapRailTransitPlanner((uri, authorization) -> {
            assertTrue(uri.toString().contains("routeType=pt&mode=rail"));
            assertTrue(uri.toString().contains("numItineraries=3"));
            assertEquals("access-token", authorization);
            return new HttpResult(200, """
                    {"plan":{"itineraries":[{"duration":2400,"walkTime":600,"transitTime":1800,"transfers":1,
                    "legs":[{"mode":"WALK","duration":300,"from":{"name":"COM3"},"to":{"name":"Kent Ridge MRT"}},
                    {"mode":"SUBWAY","duration":1800,"routeShortName":"Circle Line","from":{"name":"Kent Ridge MRT"},"to":{"name":"HarbourFront MRT"}},
                    {"mode":"WALK","duration":300,"from":{"name":"HarbourFront MRT"},"to":{"name":"VivoCity"}}]},
                    {"duration":2700,"walkTime":900,"transitTime":1800,"transfers":0}]}}""");
        }, Optional.of("access-token"));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.of(9, 30));
        var routes = lookup.routes();

        assertTrue(lookup.isAvailable());
        assertEquals(2, routes.size());
        assertEquals("Live rail route 1", routes.getFirst().name());
        assertEquals(40, routes.getFirst().totalDuration().toMinutes());
        assertEquals(1, routes.getFirst().transferCount());
        assertEquals(3, routes.getFirst().steps().size());
        assertEquals("Take Circle Line from Kent Ridge MRT to HarbourFront MRT",
                routes.getFirst().steps().get(1).description());
        assertEquals("Live rail route 2", routes.get(1).name());
        assertEquals(45, routes.get(1).totalDuration().toMinutes());
    }

    @Test
    void findRoutes_missingToken_returnsConfigurationReason() {
        var planner = new OneMapRailTransitPlanner((uri, authorization) -> {
            throw new AssertionError("No request should be made without a token.");
        }, Optional.empty());

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.now(), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing is not configured.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_incompleteItineraryFallsBackWithoutExposingAnException() {
        var planner = new OneMapRailTransitPlanner((uri, authorization) -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":600,\"transfers\":0}]}}"), Optional.of("access-token"));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }
}
