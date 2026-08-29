package timey.planner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
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

        assertEquals("Offline estimate", result.alternatives().getFirst().name());
        assertEquals(List.of("Using offline estimate: Offline",
                "Internet connection is required for an accurate travel-time estimation.",
                "Using a default 1-hour buffer before your target arrival time instead of live estimates."),
                result.messages());
    }

    @Test
    void findAlternatives_originUnavailable_doesNotResolveDestination() {
        List<String> resolvedQueries = new ArrayList<>();
        var resolver = (timey.ports.LocationResolver) query -> {
            resolvedQueries.add(query);
            if (query.equals("COM3")) {
                return LocationResolution.unavailable("Live location lookup is unavailable.");
            }
            throw new AssertionError("Destination should not be resolved after an unavailable origin.");
        };
        RailTransitPlanner railPlanner = (origin, destination, date, time) -> {
            throw new AssertionError("Live routing should not run without both locations.");
        };
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()), resolver, railPlanner, CLOCK);

        var result = planner.findAlternatives(PLAN);

        assertEquals(List.of("COM3"), resolvedQueries);
        assertEquals("Offline estimate", result.alternatives().getFirst().name());
        assertTrue(result.messages().contains(
                "Internet connection is required for an accurate travel-time estimation."));
    }

    @Test
    void findAlternatives_liveLookupUnavailable_returnsDeterministicRoutesAndReason() {
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.found(
                new ResolvedLocation(query, query + " address", 1.3, 103.8));
        RailTransitPlanner railPlanner = (origin, destination, date, time) ->
                LiveRouteLookup.unavailable("OneMap is unavailable.");
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()), resolver, railPlanner, CLOCK);

        var result = planner.findAlternatives(PLAN);

        assertEquals("Offline estimate", result.alternatives().getFirst().name());
        assertEquals("Using a default 1-hour buffer before your target arrival time instead of live estimates.",
                result.messages().getLast());
    }

    @Test
    void recommendFallbackDeparture_targetAlreadyPassed_usesNextSingaporeDay() {
        Clock afterMidnightInSingapore = Clock.fixed(Instant.parse("2026-08-20T16:30:00Z"),
                ZoneId.of("Asia/Singapore"));
        RailTransitPlanner railPlanner = (origin, destination, date, time) -> LiveRouteLookup.available(List.of());
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()),
                query -> LocationResolution.unavailable("Offline"), railPlanner, afterMidnightInSingapore);
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(0, 15), Duration.ZERO);
        var fallbackRoute = new RouteAlternative("Offline estimate", Duration.ZERO, Duration.ZERO, 0);

        var recommendation = planner.recommendFallbackDeparture(plan, fallbackRoute);

        assertEquals(LocalDateTime.of(2026, 8, 22, 0, 15), recommendation.arrivalAt());
        assertEquals(LocalDateTime.of(2026, 8, 21, 23, 15), recommendation.departureAt());
    }
}
