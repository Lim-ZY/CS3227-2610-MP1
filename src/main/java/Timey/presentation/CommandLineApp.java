package Timey.presentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Timey.application.CommutePlanningService;
import Timey.application.DepartureReminderService;
import Timey.application.LiveRailPlanningService;
import Timey.command.PlanCommand;
import Timey.command.PlanCommandParser;
import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.alert.ScheduledDepartureReminder;
import Timey.domain.location.LocationResolution;
import Timey.domain.transit.LiveRouteLookup;
import Timey.domain.transit.RouteAlternative;
import Timey.infrastructure.http.HttpResult;
import Timey.infrastructure.location.OneMapLocationResolver;
import Timey.infrastructure.transit.MockTransitPlanner;
import Timey.ports.LocationResolver;
import Timey.ports.RailTransitPlanner;
import Timey.ports.ReminderScheduler;

/** Interactive command-line presentation for Timey. */
public final class CommandLineApp {
    private static final String DIVIDER = "_______________________________________________________";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter REMINDER_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    private final BufferedReader input;
    private final PrintWriter output;
    private final PlanCommandParser planCommandParser;
    private final CommutePlanningService commutePlanningService;
    private final LocationResolver locationResolver;
    private final RailTransitPlanner railTransitPlanner;
    private final LiveRailPlanningService liveRailPlanningService;
    private final DepartureReminderService departureReminderService;
    private final Clock clock;
    private PlanCommand pendingPlan;
    private List<RouteAlternative> pendingAlternatives = List.of();

    public CommandLineApp(BufferedReader input, PrintWriter output) {
        this(input, output, new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()),
                new OneMapLocationResolver((uri, authorization) -> new HttpResult(503, ""),
                        java.util.Optional.empty()));
    }

    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver) {
        this(input, output, planCommandParser, commutePlanningService, locationResolver,
                (origin, destination, date, time) -> LiveRouteLookup.available(List.of()),
                Clock.systemDefaultZone(), (triggerAt, action) -> () -> { });
    }

    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock) {
        this(input, output, planCommandParser, commutePlanningService, locationResolver, railTransitPlanner, clock,
                (triggerAt, action) -> () -> { });
    }

    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler) {
        this.input = input;
        this.output = output;
        this.planCommandParser = planCommandParser;
        this.commutePlanningService = commutePlanningService;
        this.locationResolver = locationResolver;
        this.railTransitPlanner = railTransitPlanner;
        this.liveRailPlanningService = new LiveRailPlanningService(railTransitPlanner, clock);
        this.departureReminderService = new DepartureReminderService(reminderScheduler, clock);
        this.clock = clock;
    }

    /** Runs until the user says thanks or standard input closes. */
    public void run() {
        printWelcome();
        try {
            String command;
            while ((command = input.readLine()) != null) {
                output.println(DIVIDER);
                output.flush();
                handle(command.trim());
                if (command.trim().equalsIgnoreCase("thx")) {
                    return;
                }
                output.println(DIVIDER);
                output.flush();
                output.print("> ");
                output.flush();
            }
        } catch (IOException exception) {
            output.println("I could not read your command. Please restart Timey and try again.");
        }
    }

    private void printWelcome() {
        output.println(DIVIDER);
        output.println("  _______ _                 ");
        output.println(" |__   __(_)                ");
        output.println("    | |   _ _ __ ___   ___  _   _");
        output.println("    | |  | | '_ ` _ \\ / _ \\| | | |");
        output.println("    | |  | | | | | | |  __/| |_| |");
        output.println("    |_|  |_|_| |_| |_|\\___|\\__, |");
        output.println("                              __/ |");
        output.println("                             |___/ ");
        output.println("Hey! I'll help you to be on track today as always!");
        output.println(DIVIDER);
        output.println("Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
        output.print("> ");
        output.flush();
    }

    private void handle(String command) {
        if (command.equalsIgnoreCase("thx")) {
            output.println("Alrighty, hope you'll have a nice day ahead!");
            return;
        }
        if (command.startsWith("plan")) {
            handlePlan(command);
            return;
        }
        if (command.startsWith("choose")) {
            handleChoice(command);
            return;
        }
        if (command.equalsIgnoreCase("reminders")) {
            printReminders();
            return;
        }
        if (command.startsWith("cancel")) {
            handleCancellation(command);
            return;
        }
        output.println("I did not understand that. Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
    }

    private void handlePlan(String command) {
        try {
            PlanCommand plan = planCommandParser.parse(command);
            output.println("Got it! I have noted down your plan as follows:");
            output.println();
            output.println("From: " + plan.origin());
            output.println("To: " + plan.destination());
            output.println("Target arrival: " + plan.arrivalTime().format(TIME_FORMAT));
            output.println("Personal buffer: " + plan.buffer().toMinutes() + " minutes");
            output.println();
            List<RouteAlternative> alternatives = findAlternatives(plan);
            pendingPlan = plan;
            pendingAlternatives = alternatives;
            printAlternatives(alternatives);
            output.println();
            output.println("Choose a route with: choose 1");
        } catch (IllegalArgumentException exception) {
            output.println("I could not create that plan: " + exception.getMessage());
        }
    }

    private List<RouteAlternative> findAlternatives(PlanCommand plan) {
        LocationResolution origin = locationResolver.resolve(plan.origin());
        LocationResolution destination = locationResolver.resolve(plan.destination());
        if (origin.isFound() && destination.isFound()) {
            output.println("OneMap resolved your locations:");
            output.println("- From: " + origin.location().orElseThrow().address());
            output.println("- To: " + destination.location().orElseThrow().address());
            var liveRouteLookup = liveRailPlanningService.findAlignedRoutes(plan,
                    origin.location().orElseThrow(), destination.location().orElseThrow());
            if (liveRouteLookup.isAvailable() && !liveRouteLookup.routes().isEmpty()) {
                output.println("Live rail routes were aligned with your target arrival time.");
                output.println();
                return liveRouteLookup.routes();
            }
            if (liveRouteLookup.isAvailable()) {
                output.println("OneMap returned no live rail routes; using deterministic routes.");
            } else {
                output.println(liveRouteLookup.unavailableReason().orElseThrow()
                        + " Using deterministic routes.");
            }
        } else {
            String reason = origin.isFound() ? destination.reason() : origin.reason();
            output.println("Using deterministic routes: " + reason);
        }
        output.println();
        return commutePlanningService.findAlternatives(plan);
    }

    private void handleChoice(String command) {
        if (pendingPlan == null) {
            output.println("Please create a plan before choosing a route.");
            return;
        }
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            output.println("Choose a route by number, for example: choose 1");
            return;
        }
        try {
            int routeNumber = Integer.parseInt(parts[1]);
            if (routeNumber < 1 || routeNumber > pendingAlternatives.size()) {
                output.println("Please choose a route between 1 and " + pendingAlternatives.size() + ".");
                return;
            }
            RouteAlternative route = pendingAlternatives.get(routeNumber - 1);
            DepartureRecommendation recommendation = commutePlanningService.recommendDeparture(pendingPlan, route);
            printRecommendation(recommendation);
            scheduleReminder(recommendation);
        } catch (NumberFormatException exception) {
            output.println("Choose a route by number, for example: choose 1");
        }
    }

    private void scheduleReminder(DepartureRecommendation recommendation) {
        var reminder = departureReminderService.schedule(recommendation, () -> {
            output.println();
            output.println("Timey reminder: Please leave your desk now.");
            output.print("> ");
            output.flush();
        });
        output.println("Departure reminder automatically set for "
                + REMINDER_TIME_FORMAT.format(reminder.triggerAt().atZone(clock.getZone())) + ".");
    }

    private void printReminders() {
        List<ScheduledDepartureReminder> reminders = departureReminderService.scheduledReminders();
        if (reminders.isEmpty()) {
            output.println("You have no active departure reminders.");
            return;
        }
        output.println("Active departure reminders:");
        for (int index = 0; index < reminders.size(); index++) {
            var reminder = reminders.get(index);
            output.println((index + 1) + ". " + REMINDER_TIME_FORMAT.format(reminder.triggerAt().atZone(clock.getZone()))
                    + " — " + reminder.message());
        }
    }

    private void handleCancellation(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            output.println("Cancel a reminder by number, for example: cancel 1");
            return;
        }
        try {
            int reminderNumber = Integer.parseInt(parts[1]);
            if (departureReminderService.cancel(reminderNumber)) {
                output.println("Cancelled departure reminder " + reminderNumber + ".");
            } else {
                output.println("No active departure reminder numbered " + reminderNumber + ".");
            }
        } catch (NumberFormatException exception) {
            output.println("Cancel a reminder by number, for example: cancel 1");
        }
    }

    private void printAlternatives(List<RouteAlternative> alternatives) {
        output.println("Here are your route alternatives:");
        for (int index = 0; index < alternatives.size(); index++) {
            RouteAlternative route = alternatives.get(index);
            output.println((index + 1) + ". " + route.name() + " — "
                    + formatDuration(route.totalDuration()) + " total "
                    + "(walk " + formatDuration(route.walkingDuration())
                    + ", transit " + formatDuration(route.transitDuration())
                    + ", " + route.transferCount() + pluraliseTransfer(route.transferCount()) + ")");
            for (var step : route.steps()) {
                output.println("   - " + step.description() + " (" + formatDuration(step.duration()) + ")");
            }
        }
    }

    private String formatDuration(Duration duration) {
        return duration.toMinutes() + " minutes";
    }

    private String pluraliseTransfer(int transferCount) {
        return transferCount == 1 ? " transfer" : " transfers";
    }

    private void printRecommendation(DepartureRecommendation recommendation) {
        output.println("Great choice! Here is your departure plan:");
        output.println();
        output.println("Chosen route: " + recommendation.routeName());
        output.println("Total travel time: " + formatDuration(recommendation.travelDuration()));
        output.println("Personal buffer: " + formatDuration(recommendation.buffer()));
        output.println("Recommended departure: " + recommendation.departureTime().format(TIME_FORMAT));
        output.println();
        output.println("Please leave your desk by " + recommendation.departureTime().format(TIME_FORMAT) + ".");
    }
}
