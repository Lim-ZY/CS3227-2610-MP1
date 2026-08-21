package Timey.presentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Clock;
import java.time.LocalTime;
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
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter REMINDER_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    private final Ui ui;
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
        this.ui = new Ui(input, output);
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
        ui.printWelcome();
        try {
            String command;
            while ((command = ui.readCommand()) != null) {
                ui.printDivider();
                handle(command.trim());
                if (command.trim().equalsIgnoreCase("thx")) {
                    return;
                }
                ui.printDivider();
                ui.printPrompt();
            }
        } catch (IOException exception) {
            ui.showReadingError();
        }
    }

    private void handle(String command) {
        if (command.equalsIgnoreCase("thx")) {
            ui.println("Alrighty, hope you'll have a nice day ahead!");
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
        ui.println("I did not understand that. Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
    }

    private void handlePlan(String command) {
        try {
            PlanCommand plan = planCommandParser.parse(command);
            ui.println("Got it! I have noted down your plan as follows:");
            ui.println();
            ui.println("From: " + plan.origin());
            ui.println("To: " + plan.destination());
            ui.println("Target arrival: " + plan.arrivalTime().format(TIME_FORMAT));
            ui.println("Personal buffer: " + plan.buffer().toMinutes() + " minutes");
            ui.println();
            List<RouteAlternative> alternatives = findAlternatives(plan);
            pendingPlan = plan;
            pendingAlternatives = alternatives;
            printAlternatives(alternatives);
            ui.println();
            ui.println("Choose a route with: choose 1");
        } catch (IllegalArgumentException exception) {
            ui.println("I could not create that plan: " + exception.getMessage());
        }
    }

    private List<RouteAlternative> findAlternatives(PlanCommand plan) {
        LocationResolution origin = locationResolver.resolve(plan.origin());
        LocationResolution destination = locationResolver.resolve(plan.destination());
        if (origin.isFound() && destination.isFound()) {
            ui.println("OneMap resolved your locations:");
            ui.println("- From: " + origin.location().orElseThrow().address());
            ui.println("- To: " + destination.location().orElseThrow().address());
            var liveRouteLookup = liveRailPlanningService.findAlignedRoutes(plan,
                    origin.location().orElseThrow(), destination.location().orElseThrow());
            if (liveRouteLookup.isAvailable() && !liveRouteLookup.routes().isEmpty()) {
                ui.println("Live rail routes were aligned with your target arrival time.");
                ui.println();
                return liveRouteLookup.routes();
            }
            if (liveRouteLookup.isAvailable()) {
                ui.println("OneMap returned no live rail routes; using deterministic routes.");
            } else {
                ui.println(liveRouteLookup.unavailableReason().orElseThrow()
                        + " Using deterministic routes.");
            }
        } else {
            String reason = origin.isFound() ? destination.reason() : origin.reason();
            ui.println("Using deterministic routes: " + reason);
        }
        ui.println();
        return commutePlanningService.findAlternatives(plan);
    }

    private void handleChoice(String command) {
        if (pendingPlan == null) {
            ui.println("Please create a plan before choosing a route.");
            return;
        }
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            ui.println("Choose a route by number, for example: choose 1");
            return;
        }
        try {
            int routeNumber = Integer.parseInt(parts[1]);
            if (routeNumber < 1 || routeNumber > pendingAlternatives.size()) {
                ui.println("Please choose a route between 1 and " + pendingAlternatives.size() + ".");
                return;
            }
            RouteAlternative route = pendingAlternatives.get(routeNumber - 1);
            DepartureRecommendation recommendation = commutePlanningService.recommendDeparture(pendingPlan, route);
            printRecommendation(recommendation);
            if (recommendation.departureTime().isBefore(LocalTime.now(clock))) {
                ui.println("You have to leave now to stay on time! Good luck!");
                return;
            }
            scheduleReminder(recommendation);
        } catch (NumberFormatException exception) {
            ui.println("Choose a route by number, for example: choose 1");
        }
    }

    private void scheduleReminder(DepartureRecommendation recommendation) {
        var reminder = departureReminderService.schedule(recommendation, () -> {
            ui.println();
            ui.println("Timey reminder: Please leave your desk now.");
            ui.printPrompt();
        });
        ui.println("Departure reminder automatically set for "
                + REMINDER_TIME_FORMAT.format(reminder.triggerAt().atZone(clock.getZone())) + ".");
    }

    private void printReminders() {
        List<ScheduledDepartureReminder> reminders = departureReminderService.scheduledReminders();
        if (reminders.isEmpty()) {
            ui.println("You have no active departure reminders.");
            return;
        }
        ui.println("Active departure reminders:");
        for (int index = 0; index < reminders.size(); index++) {
            var reminder = reminders.get(index);
            ui.println((index + 1) + ". " + REMINDER_TIME_FORMAT.format(reminder.triggerAt().atZone(clock.getZone()))
                    + " — " + reminder.message());
        }
    }

    private void handleCancellation(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            ui.println("Cancel a reminder by number, for example: cancel 1");
            return;
        }
        try {
            int reminderNumber = Integer.parseInt(parts[1]);
            if (departureReminderService.cancel(reminderNumber)) {
                ui.println("Cancelled departure reminder " + reminderNumber + ".");
            } else {
                ui.println("No active departure reminder numbered " + reminderNumber + ".");
            }
        } catch (NumberFormatException exception) {
            ui.println("Cancel a reminder by number, for example: cancel 1");
        }
    }

    private void printAlternatives(List<RouteAlternative> alternatives) {
        ui.println("Here are your route alternatives:");
        for (int index = 0; index < alternatives.size(); index++) {
            RouteAlternative route = alternatives.get(index);
            ui.println((index + 1) + ". " + route.name() + " — "
                    + formatDuration(route.totalDuration()) + " total "
                    + "(walk " + formatDuration(route.walkingDuration())
                    + ", transit " + formatDuration(route.transitDuration())
                    + ", " + route.transferCount() + pluraliseTransfer(route.transferCount()) + ")");
            for (var step : route.steps()) {
                ui.println("   - " + step.description() + " (" + formatDuration(step.duration()) + ")");
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
        ui.println("Great choice! Here is your departure plan:");
        ui.println();
        ui.println("Chosen route: " + recommendation.routeName());
        ui.println("Total travel time: " + formatDuration(recommendation.travelDuration()));
        ui.println("Personal buffer: " + formatDuration(recommendation.buffer()));
        ui.println("Recommended departure: " + recommendation.departureTime().format(TIME_FORMAT));
        ui.println();
        ui.println("Please leave your desk by " + recommendation.departureTime().format(TIME_FORMAT) + ".");
    }
}
