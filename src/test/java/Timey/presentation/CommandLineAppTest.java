package Timey.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;

import org.junit.jupiter.api.Test;

import Timey.application.CommutePlanningService;
import Timey.command.PlanCommandParser;
import Timey.domain.location.LocationResolution;
import Timey.domain.location.ResolvedLocation;
import Timey.domain.transit.LiveRouteLookup;
import Timey.domain.transit.RouteAlternative;
import Timey.domain.transit.RouteStep;
import Timey.domain.transit.RouteStepMode;
import Timey.infrastructure.transit.MockTransitPlanner;
import Timey.ports.RailTransitPlanner;
import Timey.ports.ReminderScheduler;

class CommandLineAppTest {
    @Test
    void run_validPlanAndRouteChoice_displaysPlanAndFarewell() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(
                new BufferedReader(new StringReader(
                        "plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 5m\nchoose 1\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        String output = outputText.toString();
        assertTrue(output.contains("Hey! I'll help you to be on track today as always!"));
        assertTrue(output.contains("> "));
        assertTrue(output.contains("From: COM3"));
        assertTrue(output.contains("To: VivoCity"));
        assertTrue(output.contains("Target arrival: 18:30"));
        assertTrue(output.contains("Personal buffer: 5 minutes"));
        assertTrue(output.contains("1. Fastest Transit — 43 minutes total"));
        assertTrue(output.contains("2. Direct Bus — 59 minutes total"));
        assertTrue(output.contains("Chosen route: Fastest Transit"));
        assertTrue(output.contains("Recommended departure: 17:42"));
        assertTrue(output.contains("Alrighty, hope you'll have a nice day ahead!"));
    }

    @Test
    void run_invalidReplacementPlan_preservesPreviousPlan() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\n"
                        + "plan /from \"COM3\" /to \"VivoCity\" /by 18x0\nchoose 2\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        assertTrue(outputText.toString().contains("I could not create that plan:"));
        assertTrue(outputText.toString().contains("Chosen route: Direct Bus"));
    }

    @Test
    void run_invalidRouteSelection_preservesPendingPlan() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nchoose 0\nchoose one\nchoose 1\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        String output = outputText.toString();
        assertTrue(output.contains("Please choose a route between 1 and 2."));
        assertTrue(output.contains("Choose a route by number, for example: choose 1"));
        assertTrue(output.contains("Chosen route: Fastest Transit"));
    }

    @Test
    void run_emptyCommand_preservesPendingPlan() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\n\nchoose 1\nthx\n")), new PrintWriter(outputText, true));

        app.run();

        assertTrue(outputText.toString().contains("I did not understand that."));
        assertTrue(outputText.toString().contains("Chosen route: Fastest Transit"));
    }

    @Test
    void run_locationsResolved_displaysResolvedLocations() {
        var outputText = new StringWriter();
        var resolver = (Timey.ports.LocationResolver) query -> LocationResolution.found(
                new ResolvedLocation(query, query + " address", 1.3, 103.8));
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nthx\n")), new PrintWriter(outputText, true),
                new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()), resolver);

        app.run();

        assertTrue(outputText.toString().contains("OneMap resolved your locations:"));
        assertTrue(outputText.toString().contains("From: COM3 address"));
        assertTrue(outputText.toString().contains("To: VivoCity address"));
    }

    @Test
    void run_oneLocationUnresolved_displaysDeterministicRoutes() {
        var outputText = new StringWriter();
        var resolver = (Timey.ports.LocationResolver) query -> query.equals("COM3")
                ? LocationResolution.found(new ResolvedLocation(query, "COM3 address", 1.3, 103.8))
                : LocationResolution.unavailable("OneMap could not find \"VivoCity\".");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nthx\n")), new PrintWriter(outputText, true),
                new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()), resolver);

        app.run();

        assertTrue(outputText.toString().contains("Using deterministic routes: OneMap could not find \"VivoCity\"."));
        assertTrue(outputText.toString().contains("1. Fastest Transit"));
    }

    @Test
    void run_liveRoutesAvailable_displaysItemisedLiveRoutes() {
        var outputText = new StringWriter();
        var resolver = (Timey.ports.LocationResolver) query -> LocationResolution.found(
                new ResolvedLocation(query, query + " address", 1.3, 103.8));
        RailTransitPlanner railPlanner = (origin, destination, date, time) -> LiveRouteLookup.available(
                List.of(new RouteAlternative("Live rail route 1", Duration.ofMinutes(6), Duration.ofMinutes(30), 1,
                                List.of(new RouteStep(RouteStepMode.WALK, "COM3", "Kent Ridge MRT", "walking", Duration.ofMinutes(6)),
                                        new RouteStep(RouteStepMode.RAIL, "Kent Ridge MRT", "HarbourFront MRT", "Circle Line", Duration.ofMinutes(30)))),
                        new RouteAlternative("Live rail route 2", Duration.ofMinutes(4), Duration.ofMinutes(35), 0)));
        var clock = Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore"));
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nthx\n")), new PrintWriter(outputText, true),
                new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()), resolver,
                railPlanner, clock);

        app.run();

        assertTrue(outputText.toString().contains("Live rail routes were aligned with your target arrival time."));
        assertTrue(outputText.toString().contains("1. Live rail route 1 — 36 minutes total"));
        assertTrue(outputText.toString().contains("2. Live rail route 2 — 39 minutes total"));
        assertTrue(outputText.toString().contains("- Walk from COM3 to Kent Ridge MRT (6 minutes)"));
        assertTrue(outputText.toString().contains("- Take Circle Line from Kent Ridge MRT to HarbourFront MRT (30 minutes)"));
    }

    @Test
    void run_routeChosen_schedulesDepartureReminderAutomatically() {
        var outputText = new StringWriter();
        var scheduledAt = new AtomicReference<java.time.Instant>();
        ReminderScheduler scheduler = (triggerAt, action) -> scheduledAt.set(triggerAt);
        var clock = Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore"));
        var resolver = (Timey.ports.LocationResolver) query -> LocationResolution.unavailable("Offline");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nchoose 1\nthx\n")),
                new PrintWriter(outputText, true), new PlanCommandParser(),
                new CommutePlanningService(new MockTransitPlanner()), resolver,
                (origin, destination, date, time) -> LiveRouteLookup.available(List.of()), clock, scheduler);

        app.run();

        assertEquals(Instant.parse("2026-08-21T09:37:00Z"), scheduledAt.get());
        assertTrue(outputText.toString().contains("Departure reminder automatically set for 2026-08-21 17:37."));
    }

    @Test
    void run_removedRemindCommand_displaysUnknownCommandGuidance() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader("remind\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        assertTrue(outputText.toString().contains("I did not understand that."));
    }
}
