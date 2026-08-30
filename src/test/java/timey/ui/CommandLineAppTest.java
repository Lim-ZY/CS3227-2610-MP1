package timey.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import timey.domain.alert.SavedPlan;
import timey.domain.location.LocationResolution;
import timey.domain.location.ResolvedLocation;
import timey.domain.transit.LiveRouteLookup;
import timey.domain.transit.RouteAlternative;
import timey.domain.transit.RouteStep;
import timey.domain.transit.RouteStepMode;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;
import timey.infrastructure.transit.MockTransitPlanner;
import timey.parser.PlanCommandParser;
import timey.planner.CommutePlanningService;
import timey.ports.RailTransitPlanner;
import timey.ports.ReminderScheduler;

class CommandLineAppTest {
    @Test
    void executeCommand_selectedFutureRoute_savesPlanThroughInjectedStore() {
        var outputText = new StringWriter();
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        var app = new CommandLineApp(
                new ConsoleUi(new BufferedReader(new StringReader("")), new PrintWriter(outputText, true)),
                new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()),
                query -> LocationResolution.unavailable("Offline"), availableRailPlanner(),
                Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore")),
                noOpReminderScheduler(), new InMemoryFixedCommuteStore(),
                plans -> savedPlanLists.add(List.copyOf(plans)));

        app.executeCommand("plan /from \"COM3\" /to \"VivoCity\" /by 1830");
        app.executeCommand("choose 1");

        assertEquals(1, savedPlanLists.size());
        assertEquals("COM3", savedPlanLists.getFirst().getFirst().origin());
        assertEquals("VivoCity", savedPlanLists.getFirst().getFirst().destination());
    }

    @Test
    void executeCommand_addThenPlan_matchingFixedTimingIsFirstAlternative() {
        var outputText = new StringWriter();
        var fixedCommutes = new InMemoryFixedCommuteStore();
        var app = new CommandLineApp(new BufferedReader(new StringReader("")), new PrintWriter(outputText, true),
                new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()),
                query -> LocationResolution.unavailable("Offline"), availableRailPlanner(),
                Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore")),
                noOpReminderScheduler(), fixedCommutes);

        app.executeCommand("add /from \"COM3\" /to \"VivoCity\" /dur 1h30m");
        var result = app.executeCommand("plan /from \"COM3\" /to \"VivoCity\" /by 1830");

        assertEquals("Saved timing", result.dashboardState().alternatives().getFirst().name());
        assertEquals(Duration.ofMinutes(90), result.dashboardState().alternatives().getFirst().totalDuration());
        assertTrue(outputText.toString().contains("Saved fixed timing from COM3 to VivoCity: 90 minutes."));
        assertTrue(outputText.toString().contains("Your saved fixed timing is available as route 1."));
    }

    @Test
    void executeCommand_planThenChoose_preservesCommandStateOutsideTerminalLoop() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader("")), new PrintWriter(outputText, true));

        var planResult = app.executeCommand("plan /from \"COM3\" /to \"VivoCity\" /by 1830");
        var chooseResult = app.executeCommand("choose 1");

        assertTrue(!planResult.sessionEnded());
        assertTrue(!chooseResult.sessionEnded());
        assertEquals("COM3", planResult.dashboardState().plan().orElseThrow().getOrigin());
        assertEquals(1, planResult.dashboardState().alternatives().size());
        assertEquals("Offline estimate", chooseResult.dashboardState().recommendation().orElseThrow().routeName());
        assertEquals(1, planResult.dashboardState().alternatives().size());
        assertEquals(1, chooseResult.dashboardState().alternatives().size());
        assertTrue(outputText.toString().contains("1. Offline estimate"));
        assertTrue(outputText.toString().contains("Chosen route: Offline estimate"));
    }

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
        assertTrue(output.contains("1. Offline estimate — 60 minutes total"));
        assertTrue(output.contains("Chosen route: Offline estimate"));
        assertTrue(output.contains("Recommended departure: 17:30"));
        assertTrue(output.contains("Alrighty, hope you'll have a nice day ahead!"));
    }

    @Test
    void run_invalidReplacementPlan_preservesPreviousPlan() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\n"
                        + "plan /from \"COM3\" /to \"VivoCity\" /by 18x0\nchoose 1\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        assertTrue(outputText.toString().contains("I could not create that plan:"));
        assertTrue(outputText.toString().contains("Chosen route: Offline estimate"));
    }

    @Test
    void run_invalidRouteSelection_preservesPendingPlan() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nchoose 0\nchoose one\nchoose 1\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        String output = outputText.toString();
        assertTrue(output.contains("Please choose a route between 1 and 1."));
        assertTrue(output.contains("Choose a route by number, for example: choose 1"));
        assertTrue(output.contains("Chosen route: Offline estimate"));
    }

    @Test
    void run_emptyCommand_preservesPendingPlan() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\n\nchoose 1\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        assertTrue(outputText.toString().contains("I did not understand that."));
        assertTrue(outputText.toString().contains("Chosen route: Offline estimate"));
    }

    @Test
    void run_locationsResolved_displaysResolvedLocations() {
        var outputText = new StringWriter();
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.found(
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
        var resolver = (timey.ports.LocationResolver) query -> query.equals("COM3")
                ? LocationResolution.found(new ResolvedLocation(query, "COM3 address", 1.3, 103.8))
                : LocationResolution.unavailable("OneMap could not find \"VivoCity\".");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nthx\n")), new PrintWriter(outputText, true),
                new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()), resolver);

        app.run();

        assertTrue(outputText.toString().contains("Using offline estimate: OneMap could not find \"VivoCity\"."));
        assertTrue(outputText.toString().contains("1. Offline estimate"));
    }

    @Test
    void run_workerUnreachable_displaysInternetGuidanceAndOfflineEstimate() {
        var outputText = new StringWriter();
        var resolver = (timey.ports.LocationResolver) query ->
                LocationResolution.unreachable("Worker could not be reached.");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"Blk 127 Rivervale Street\" /to \"Compass One\" /by 1800\nthx\n")),
                new PrintWriter(outputText, true), new PlanCommandParser(),
                new CommutePlanningService(new MockTransitPlanner()), resolver);

        app.run();

        String output = outputText.toString();
        assertTrue(output.contains("I'm so sorry, I need Internet connection to help you plan your routes "
                + "accurately."));
        assertTrue(output.contains("Please reconnect to the Internet for more accurate estimates."));
        assertTrue(output.contains("Using a default 1-hour buffer before your target arrival time instead of live "
                + "estimates."));
        assertTrue(output.contains("1. Offline estimate — 60 minutes total"));
        assertTrue(output.contains("Choose a route with: choose 1"));
        assertFalse(output.contains("Worker could not be reached."));
    }

    @Test
    void run_routeNotFound_displaysOfflineEstimateAndFixedTimingSuggestion() {
        var outputText = new StringWriter();
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.found(
                new ResolvedLocation(query, query + " address", 1.3, 103.8));
        RailTransitPlanner railPlanner = (origin, destination, date, time) ->
                LiveRouteLookup.unavailable(404, "Unable to get MRT route");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nthx\n")), new PrintWriter(outputText, true),
                new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()), resolver,
                railPlanner, Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore")));

        app.run();

        String output = outputText.toString();
        assertTrue(output.contains("I'm so sorry, OneMap failed to find a suitable route."));
        assertTrue(output.contains("Using a default 1-hour buffer before your target arrival time instead of live "
                + "estimates."));
        assertTrue(output.contains("1. Offline estimate — 60 minutes total"));
        assertTrue(output.contains("Choose a route with: choose 1"));
        assertTrue(output.contains("(Perhaps use `add` later to save this commute route for future reference?)"));
        assertTrue(output.indexOf("Choose a route with: choose 1")
                < output.indexOf("(Perhaps use `add` later to save this commute route for future reference?)"));
        assertFalse(output.contains("Unable to get MRT route"));
    }

    @Test
    void executeCommand_runtimePlanningFailure_showsSafeErrorAndPreservesEmptyState() {
        var outputText = new StringWriter();
        var resolver = (timey.ports.LocationResolver) query -> {
            throw new IllegalStateException("Provider secret failure");
        };
        var app = new CommandLineApp(new BufferedReader(new StringReader("")), new PrintWriter(outputText, true),
                new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()), resolver);

        var result = app.executeCommand("plan /from \"COM3\" /to \"VivoCity\" /by 1830");

        assertTrue(!result.sessionEnded());
        assertTrue(outputText.toString().contains("Your current plan has not changed."));
        assertTrue(outputText.toString().contains("Check your internet connection or saved data"));
        assertTrue(!outputText.toString().contains("Provider secret failure"));
        assertTrue(result.dashboardState().plan().isEmpty());
        assertTrue(result.dashboardState().alternatives().isEmpty());
    }

    @Test
    void run_liveRoutesAvailable_displaysItemisedLiveRoutes() {
        var outputText = new StringWriter();
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.found(
                new ResolvedLocation(query, query + " address", 1.3, 103.8));
        RailTransitPlanner railPlanner = (origin, destination, date, time) -> LiveRouteLookup.available(
                List.of(new RouteAlternative("Live rail route 1", Duration.ofMinutes(6), Duration.ofMinutes(30), 1,
                                List.of(new RouteStep(RouteStepMode.WALK, "COM3", "Kent Ridge MRT", "walking",
                                                Duration.ofMinutes(6)),
                                        new RouteStep(RouteStepMode.RAIL, "Kent Ridge MRT", "HarbourFront MRT",
                                                "Circle Line", Duration.ofMinutes(30)))),
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
        assertTrue(outputText.toString()
                .contains("- Take Circle Line from Kent Ridge MRT to HarbourFront MRT (30 minutes)"));
    }

    @Test
    void run_routeChosen_schedulesDepartureReminderAutomatically() {
        var outputText = new StringWriter();
        var scheduledAt = new AtomicReference<java.time.Instant>();
        ReminderScheduler scheduler = (triggerAt, action) -> {
            scheduledAt.set(triggerAt);
            return () -> { };
        };
        var clock = Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore"));
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.unavailable("Offline");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nchoose 1\nreminders\nthx\n")),
                new PrintWriter(outputText, true), new PlanCommandParser(),
                new CommutePlanningService(new MockTransitPlanner()), resolver, availableRailPlanner(), clock,
                scheduler);

        app.run();

        assertEquals(Instant.parse("2026-08-21T09:30:00Z"), scheduledAt.get());
        assertTrue(outputText.toString().contains("Departure reminder automatically set for 2026-08-21 17:30."));
        assertTrue(outputText.toString().contains("Active departure reminders:"));
        assertTrue(outputText.toString().contains("1. 2026-08-21 17:30 — Timey reminder: Please leave your desk now."));
    }

    @Test
    void run_scheduledReminder_displaysConsoleNotification() {
        var outputText = new StringWriter();
        var scheduledAction = new AtomicReference<Runnable>();
        ReminderScheduler scheduler = (triggerAt, action) -> {
            scheduledAction.set(action);
            return () -> { };
        };
        var clock = Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore"));
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.unavailable("Offline");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nchoose 1\nthx\n")),
                new PrintWriter(outputText, true), new PlanCommandParser(),
                new CommutePlanningService(new MockTransitPlanner()), resolver, availableRailPlanner(), clock,
                scheduler);

        app.run();
        scheduledAction.get().run();

        assertTrue(outputText.toString().contains("Timey reminder: Please leave your desk now."));
    }

    @Test
    void run_removedRemindCommand_displaysUnknownCommandGuidance() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader("remind\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        assertTrue(outputText.toString().contains("I did not understand that."));
    }

    @Test
    void run_noScheduledReminders_displaysEmptyReminderMessage() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(new BufferedReader(new StringReader("reminders\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        assertTrue(outputText.toString().contains("You have no active departure reminders."));
    }

    @Test
    void run_cancelActiveReminder_removesReminderFromList() {
        var outputText = new StringWriter();
        ReminderScheduler scheduler = (triggerAt, action) -> () -> { };
        var clock = Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore"));
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.unavailable("Offline");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nchoose 1\ncancel 1\nreminders\nthx\n")),
                new PrintWriter(outputText, true), new PlanCommandParser(),
                new CommutePlanningService(new MockTransitPlanner()), resolver, availableRailPlanner(), clock,
                scheduler);

        app.run();

        assertTrue(outputText.toString().contains("Cancelled departure reminder 1."));
        assertTrue(outputText.toString().contains("You have no active departure reminders."));
    }

    @Test
    void run_departureTimePassed_displaysLeaveNowMessageWithoutSchedulingReminder() {
        var outputText = new StringWriter();
        var scheduledAt = new AtomicReference<java.time.Instant>();
        ReminderScheduler scheduler = (triggerAt, action) -> {
            scheduledAt.set(triggerAt);
            return () -> { };
        };
        var clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneId.of("Asia/Singapore"));
        var resolver = (timey.ports.LocationResolver) query -> LocationResolution.unavailable("Offline");
        var app = new CommandLineApp(new BufferedReader(new StringReader(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830\nchoose 1\nreminders\nthx\n")),
                new PrintWriter(outputText, true), new PlanCommandParser(),
                new CommutePlanningService(new MockTransitPlanner()), resolver, availableRailPlanner(), clock,
                scheduler);

        app.run();

        assertNull(scheduledAt.get());
        assertTrue(outputText.toString().contains("You have to leave now to stay on time! Good luck!"));
        assertTrue(outputText.toString().contains("You have no active departure reminders."));
    }

    private static RailTransitPlanner availableRailPlanner() {
        return (origin, destination, date, time) -> LiveRouteLookup.available(List.of());
    }

    private static ReminderScheduler noOpReminderScheduler() {
        return (triggerAt, action) -> () -> { };
    }
}
