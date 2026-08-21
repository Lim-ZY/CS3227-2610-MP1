package Timey.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import Timey.command.PlanCommand;
import Timey.domain.location.ResolvedLocation;
import Timey.domain.transit.LiveRouteLookup;
import Timey.domain.transit.RouteAlternative;
import Timey.ports.RailTransitPlanner;

class LiveRailPlanningServiceTest {
    private static final ResolvedLocation ORIGIN = new ResolvedLocation("COM3", "COM3", 1.294, 103.773);
    private static final ResolvedLocation DESTINATION = new ResolvedLocation("VivoCity", "VivoCity", 1.264, 103.822);
    private static final Clock SINGAPORE_MORNING = Clock.fixed(Instant.parse("2026-08-21T02:00:00Z"),
            ZoneId.of("Asia/Singapore"));

    @Test
    void refreshesRoutesAtTheCalculatedLeaveByTime() {
        List<String> requestedTimes = new ArrayList<>();
        RailTransitPlanner planner = (origin, destination, date, time) -> {
            requestedTimes.add(date + " " + time);
            return LiveRouteLookup.available(List.of(new RouteAlternative("Rail", Duration.ofMinutes(8),
                    Duration.ofMinutes(35), 1)));
        };
        var service = new LiveRailPlanningService(planner, SINGAPORE_MORNING);
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ofMinutes(10));

        var result = service.findAlignedRoutes(plan, ORIGIN, DESTINATION);

        assertTrue(result.isAvailable());
        assertEquals(List.of("2026-08-21 18:30", "2026-08-21 17:37"), requestedTimes);
    }

    @Test
    void usesTomorrowWhenTheTargetTimeHasAlreadyPassedToday() {
        List<LocalDate> requestedDates = new ArrayList<>();
        RailTransitPlanner planner = (origin, destination, date, time) -> {
            requestedDates.add(date);
            return LiveRouteLookup.unavailable("No route");
        };
        var service = new LiveRailPlanningService(planner, SINGAPORE_MORNING);
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(9, 0), Duration.ofMinutes(10));

        service.findAlignedRoutes(plan, ORIGIN, DESTINATION);

        assertEquals(List.of(LocalDate.of(2026, 8, 22)), requestedDates);
    }

    @Test
    void usesTomorrowWhenTheTargetTimeIsExactlyNow() {
        List<LocalDate> requestedDates = new ArrayList<>();
        RailTransitPlanner planner = (origin, destination, date, time) -> {
            requestedDates.add(date);
            return LiveRouteLookup.unavailable("No route");
        };
        var service = new LiveRailPlanningService(planner, SINGAPORE_MORNING);
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(10, 0), Duration.ofMinutes(10));

        service.findAlignedRoutes(plan, ORIGIN, DESTINATION);

        assertEquals(List.of(LocalDate.of(2026, 8, 22)), requestedDates);
    }

    @Test
    void stopsAfterTheProbeWhenNoRouteIsAvailable() {
        List<LocalTime> requestedTimes = new ArrayList<>();
        RailTransitPlanner planner = (origin, destination, date, time) -> {
            requestedTimes.add(time);
            return LiveRouteLookup.available(List.of());
        };
        var service = new LiveRailPlanningService(planner, SINGAPORE_MORNING);
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ofMinutes(10));

        var result = service.findAlignedRoutes(plan, ORIGIN, DESTINATION);

        assertTrue(result.isAvailable());
        assertTrue(result.routes().isEmpty());
        assertEquals(List.of(LocalTime.of(18, 30)), requestedTimes);
    }

    @Test
    void refreshesOnThePreviousDateWhenTheCalculatedDepartureCrossesMidnight() {
        List<String> requestedTimes = new ArrayList<>();
        RailTransitPlanner planner = (origin, destination, date, time) -> {
            requestedTimes.add(date + " " + time);
            return LiveRouteLookup.available(List.of(new RouteAlternative("Rail", Duration.ofMinutes(20),
                    Duration.ofMinutes(40), 0)));
        };
        var nearMidnight = Clock.fixed(Instant.parse("2026-08-20T15:00:00Z"), ZoneId.of("Asia/Singapore"));
        var service = new LiveRailPlanningService(planner, nearMidnight);
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(0, 10), Duration.ofMinutes(10));

        service.findAlignedRoutes(plan, ORIGIN, DESTINATION);

        assertEquals(List.of("2026-08-21 00:10", "2026-08-20 23:00"), requestedTimes);
    }
}
