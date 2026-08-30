package timey.infrastructure.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import timey.domain.location.ResolvedLocation;
import timey.infrastructure.http.HttpResult;

class OneMapRailTransitPlannerTest {
    private static final ResolvedLocation COM3 = new ResolvedLocation("COM3", "COM3", 1.294, 103.773);
    private static final ResolvedLocation VIVOCITY = new ResolvedLocation("VivoCity", "VivoCity", 1.264, 103.822);

    @Test
    void findRoutes_validItineraries_mapsEveryItinerary() {
        var planner = new OneMapRailTransitPlanner(uri -> {
            assertTrue(uri.toString().startsWith("https://timey.example.workers.dev/v1/rail-route?"));
            assertTrue(uri.toString().contains("start=1.294,103.773"));
            assertTrue(uri.toString().contains("end=1.264,103.822"));
            return new HttpResult(200, """
                    {"plan":{"itineraries":[{"duration":2400,"walkTime":600,"transitTime":1800,"transfers":1,
                    "legs":[{"mode":"WALK","duration":300,"from":{"name":"COM3"},"to":{"name":"Kent Ridge MRT"}},
                    {"mode":"SUBWAY","duration":1800,"routeShortName":"Circle Line",
                    "from":{"name":"Kent Ridge MRT"},"to":{"name":"HarbourFront MRT"}},
                    {"mode":"WALK","duration":300,"from":{"name":"HarbourFront MRT"},"to":{"name":"VivoCity"}}]},
                    {"duration":2700,"walkTime":900,"transitTime":1800,"transfers":0}]}}""");
        }, Optional.of(URI.create("https://timey.example.workers.dev")));

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
    void findRoutes_serviceNotConfigured_returnsConfigurationReason() {
        var planner = new OneMapRailTransitPlanner(uri -> {
            throw new AssertionError("No request should be made without a service URL.");
        }, Optional.empty());

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.now(), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("Live rail routing is not configured.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_nonSuccessResponse_returnsProviderFailureReason() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(503, "{}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing is temporarily unavailable.", lookup.unavailableReason().orElseThrow());
        assertFalse(lookup.isLiveDataServiceUnreachable());
        assertEquals(503, lookup.responseStatusCode().orElseThrow());
    }

    @Test
    void findRoutes_errorResponse_retainsStatusAndWorkerError() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(404,
                "{\"error\":\"Unable to get MRT route\"}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("Unable to get MRT route", lookup.unavailableReason().orElseThrow());
        assertEquals(404, lookup.responseStatusCode().orElseThrow());
    }

    @Test
    void findRoutes_requestFails_returnsTemporaryUnavailableReason() {
        var planner = new OneMapRailTransitPlanner(uri -> {
            throw new IllegalStateException("Connection timed out");
        }, Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing timed out or is temporarily unavailable.",
                lookup.unavailableReason().orElseThrow());
        assertTrue(lookup.isLiveDataServiceUnreachable());
        assertTrue(lookup.responseStatusCode().isEmpty());
    }

    @Test
    void findRoutes_malformedJson_returnsUnreadableResponseReason() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(200, "not-json"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an unreadable response.", lookup.unavailableReason().orElseThrow());
        assertFalse(lookup.isLiveDataServiceUnreachable());
    }

    @Test
    void findRoutes_nullResponseBody_returnsUnreadableResponseReason() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(200, null),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an unreadable response.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_railLegUsesRouteField_mapsItemisedRailStep() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(200, """
                {"plan":{"itineraries":[{"walkTime":0,"transitTime":1800,"transfers":0,
                "legs":[{"mode":"SUBWAY","duration":1800,"route":"Circle Line",
                "from":{"name":"Kent Ridge MRT"},"to":{"name":"HarbourFront MRT"}}]}]}}"""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(lookup.isAvailable());
        assertEquals("Take Circle Line from Kent Ridge MRT to HarbourFront MRT",
                lookup.routes().getFirst().steps().getFirst().description());
    }

    @Test
    void findRoutes_incompleteItinerary_returnsFallbackWithoutException() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":600,\"transfers\":0}]}}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_negativeDuration_returnsFallbackWithoutException() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":-1,\"transitTime\":1800,\"transfers\":0}]}}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_fractionalDuration_returnsFallbackWithoutException() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":600.5,\"transitTime\":1800,\"transfers\":0}]}}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_excessiveTransfers_returnsFallbackWithoutException() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":600,\"transitTime\":1800,\"transfers\":11}]}}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_emptyResponse_returnsInvalidResponseReason() {
        var planner = new OneMapRailTransitPlanner(uri -> new HttpResult(200, ""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an invalid response.", lookup.unavailableReason().orElseThrow());
    }
}
