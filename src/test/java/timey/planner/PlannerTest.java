package timey.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import timey.command.PlanCommand;
import timey.domain.location.LocationResolution;
import timey.domain.location.ResolvedLocation;
import timey.domain.transit.LiveRouteLookup;
import timey.domain.transit.RouteAlternative;
import timey.infrastructure.transit.MockTransitPlanner;
import timey.ports.RailTransitPlanner;

class PlannerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore"));
    private static final PlanCommand PLAN = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30),
            Duration.ofMinutes(10));

    @Test
    void findAlternatives_liveRoutesAvailable_returnsLiveRoutesAndResolutionMessages() {
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.found(
                new ResolvedLocation(query, query + " address", 1.3, 103.8));
        RouteAlternative liveRoute = new RouteAlternative("Live rail", Duration.ofMinutes(5),
                Duration.ofMinutes(30), 0);
        RailTransitPlanner railPlanner = (origin, destination, date, time) -> LiveRouteLookup.available(
                List.of(liveRoute));
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()), resolver, railPlanner, CLOCK);

        var result = planner.findAlternatives(PLAN);

        assertEquals(List.of(liveRoute), result.alternatives());
        assertEquals(List.of("OneMap resolved your locations:", "- From: COM3 address", "- To: VivoCity address",
                "Live rail routes were aligned with your target arrival time."), result.messages());
    }

    @Test
    void findAlternatives_locationUnavailable_returnsDeterministicRoutesAndReason() {
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.unavailable("Offline");
        RailTransitPlanner railPlanner = (origin, destination, date, time) -> LiveRouteLookup.available(List.of());
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()), resolver, railPlanner, CLOCK);

        var result = planner.findAlternatives(PLAN);

        assertEquals("Fastest Transit", result.alternatives().getFirst().name());
        assertEquals(List.of("Using deterministic routes: Offline"), result.messages());
    }

    @Test
    void findAlternatives_liveLookupUnavailable_returnsDeterministicRoutesAndReason() {
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.found(
                new ResolvedLocation(query, query + " address", 1.3, 103.8));
        RailTransitPlanner railPlanner = (origin, destination, date, time) ->
                LiveRouteLookup.unavailable("OneMap is unavailable.");
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()), resolver, railPlanner, CLOCK);

        var result = planner.findAlternatives(PLAN);

        assertEquals("Fastest Transit", result.alternatives().getFirst().name());
        assertEquals("OneMap is unavailable. Using deterministic routes.", result.messages().getLast());
    }
}
