package timey.infrastructure.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import timey.domain.location.ResolvedLocation;
import timey.domain.transit.RouteStepMode;
import timey.infrastructure.http.HttpResult;

class OneMapTransitPlannerTest {
    private static final ResolvedLocation COM3 = new ResolvedLocation("COM3", "COM3", 1.294, 103.773);
    private static final ResolvedLocation VIVOCITY = new ResolvedLocation("VivoCity", "VivoCity", 1.264, 103.822);

    @Test
    void findRoutes_validItineraries_mapsEveryItinerary() {
        var planner = new OneMapTransitPlanner(uri -> {
            assertTrue(uri.toString().startsWith("https://timey.example.workers.dev/v1/transit-route?"));
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
        assertEquals("Live public transport route 1", routes.getFirst().name());
        assertEquals(40, routes.getFirst().totalDuration().toMinutes());
        assertEquals(1, routes.getFirst().transferCount());
        assertEquals(3, routes.getFirst().steps().size());
        assertEquals(RouteStepMode.RAIL, routes.getFirst().steps().get(1).mode());
        assertEquals("Take Circle Line from Kent Ridge MRT to HarbourFront MRT",
                routes.getFirst().steps().get(1).description());
        assertEquals("Live public transport route 2", routes.get(1).name());
        assertEquals(45, routes.get(1).totalDuration().toMinutes());
    }

    @Test
    void findRoutes_serviceNotConfigured_returnsConfigurationReason() {
        var planner = new OneMapTransitPlanner(uri -> {
            throw new AssertionError("No request should be made without a service URL.");
        }, Optional.empty());

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.now(), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("Live public transport routing is not configured.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_nonSuccessResponse_returnsProviderFailureReason() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(503, "{}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing is temporarily unavailable.", lookup.unavailableReason().orElseThrow());
        assertFalse(lookup.isLiveDataServiceUnreachable());
        assertEquals(503, lookup.responseStatusCode().orElseThrow());
    }

    @Test
    void findRoutes_errorResponse_retainsStatusAndWorkerError() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(404,
                "{\"error\":\"Unable to get MRT route\"}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("Unable to get MRT route", lookup.unavailableReason().orElseThrow());
        assertEquals(404, lookup.responseStatusCode().orElseThrow());
    }

    @Test
    void findRoutes_requestFails_returnsTemporaryUnavailableReason() {
        var planner = new OneMapTransitPlanner(uri -> {
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
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200, "not-json"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an unreadable response.", lookup.unavailableReason().orElseThrow());
        assertFalse(lookup.isLiveDataServiceUnreachable());
    }

    @Test
    void findRoutes_nullResponseBody_returnsUnreadableResponseReason() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200, null),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an unreadable response.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_railLegUsesRouteField_mapsItemisedRailStep() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200, """
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
    void findRoutes_busLeg_mapsItemisedBusStep() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200, """
                {"plan":{"itineraries":[{"walkTime":0,"transitTime":900,"transfers":0,
                "legs":[{"mode":"BUS","duration":900,"routeShortName":"95",
                "from":{"name":"NUS Kent Ridge Terminal"},"to":{"name":"Clementi MRT"}}]}]}}"""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(lookup.isAvailable());
        var step = lookup.routes().getFirst().steps().getFirst();
        assertEquals(RouteStepMode.BUS, step.mode());
        assertEquals("Take bus 95 from NUS Kent Ridge Terminal to Clementi MRT", step.description());
    }

    @Test
    void findRoutes_mixedWalkBusAndRailLegs_preservesEverySupportedMode() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200, """
                {"plan":{"itineraries":[{"walkTime":300,"transitTime":2100,"transfers":1,
                "legs":[{"mode":"WALK","duration":300,"from":{"name":"COM3"},
                "to":{"name":"Kent Ridge MRT"}},{"mode":"BUS","duration":900,"routeShortName":"95",
                "from":{"name":"NUS Kent Ridge Terminal"},"to":{"name":"Clementi MRT"}},
                {"mode":"SUBWAY","duration":1200,"routeShortName":"East West Line",
                "from":{"name":"Clementi MRT"},"to":{"name":"Outram Park MRT"}}]}]}}"""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);
        var steps = lookup.routes().getFirst().steps();

        assertTrue(lookup.isAvailable());
        assertEquals(List.of(RouteStepMode.WALK, RouteStepMode.BUS, RouteStepMode.RAIL),
                steps.stream().map(step -> step.mode()).toList());
        assertEquals("Take bus 95 from NUS Kent Ridge Terminal to Clementi MRT", steps.get(1).description());
    }

    @Test
    void findRoutes_unknownLegMode_omitsLegWithoutMislabelingItAsRail() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200, """
                {"plan":{"itineraries":[{"walkTime":0,"transitTime":900,"transfers":0,
                "legs":[{"mode":"FERRY","duration":900,"routeShortName":"Island Service",
                "from":{"name":"HarbourFront"},"to":{"name":"Pulau Ubin"}}]}]}}"""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(lookup.isAvailable());
        assertTrue(lookup.routes().getFirst().steps().isEmpty());
    }

    @Test
    void findRoutes_transitLegWithoutService_omitsOnlyTheMalformedLeg() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200, """
                {"plan":{"itineraries":[{"walkTime":300,"transitTime":900,"transfers":0,
                "legs":[{"mode":"WALK","duration":300,"from":{"name":"COM3"},
                "to":{"name":"Kent Ridge MRT"}},{"mode":"BUS","duration":900,
                "from":{"name":"NUS Kent Ridge Terminal"},"to":{"name":"Clementi MRT"}}]}]}}"""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(lookup.isAvailable());
        assertEquals(1, lookup.routes().getFirst().steps().size());
        assertEquals(RouteStepMode.WALK, lookup.routes().getFirst().steps().getFirst().mode());
    }

    @Test
    void findRoutes_incompleteItinerary_returnsFallbackWithoutException() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":600,\"transfers\":0}]}}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_negativeDuration_returnsFallbackWithoutException() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":-1,\"transitTime\":1800,\"transfers\":0}]}}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_fractionalDuration_returnsFallbackWithoutException() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":600.5,\"transitTime\":1800,\"transfers\":0}]}}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_excessiveTransfers_returnsFallbackWithoutException() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200,
                "{\"plan\":{\"itineraries\":[{\"walkTime\":600,\"transitTime\":1800,\"transfers\":11}]}}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an incomplete itinerary.", lookup.unavailableReason().orElseThrow());
    }

    @Test
    void findRoutes_emptyResponse_returnsInvalidResponseReason() {
        var planner = new OneMapTransitPlanner(uri -> new HttpResult(200, ""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var lookup = planner.findRoutes(COM3, VIVOCITY, LocalDate.of(2026, 8, 21), LocalTime.NOON);

        assertTrue(!lookup.isAvailable());
        assertEquals("OneMap routing returned an invalid response.", lookup.unavailableReason().orElseThrow());
    }
}
