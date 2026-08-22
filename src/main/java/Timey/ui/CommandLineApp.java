package Timey.ui;

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
import Timey.application.Planner;
import Timey.parser.CommandParser;
import Timey.parser.PlanCommand;
import Timey.parser.PlanCommandParser;
import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.alert.ScheduledDepartureReminder;
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
    private final CommandParser commandParser;
    private final CommutePlanningService commutePlanningService;
    private final Planner planner;
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
        this(new Ui(input, output), planCommandParser, commutePlanningService, locationResolver,
                (origin, destination, date, time) -> LiveRouteLookup.available(List.of()),
                Clock.systemDefaultZone(), (triggerAt, action) -> () -> { });
    }

    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock) {
        this(new Ui(input, output), planCommandParser, commutePlanningService, locationResolver, railTransitPlanner, clock,
                (triggerAt, action) -> () -> { });
    }

    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler) {
        this(new Ui(input, output), planCommandParser, commutePlanningService, locationResolver, railTransitPlanner, clock,
                reminderScheduler);
    }

    public CommandLineApp(Ui ui, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler) {
        this.ui = ui;
        this.commandParser = new CommandParser(planCommandParser);
        this.commutePlanningService = commutePlanningService;
        this.planner = new Planner(commutePlanningService, locationResolver, railTransitPlanner, clock);
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
                if (handle(command)) {
                    return;
                }
                ui.printDivider();
                ui.printPrompt();
            }
        } catch (IOException exception) {
            ui.showReadingError();
        }
    }

    private boolean handle(String input) {
        try {
            CommandParser.Command command = commandParser.parse(input);
            return switch (command.type()) {
            case THANKS -> {
                ui.println("Alrighty, hope you'll have a nice day ahead!");
                yield true;
            }
            case PLAN -> {
                handlePlan(command.plan());
                yield false;
            }
            case CHOOSE -> {
                handleChoice(command.number());
                yield false;
            }
            case REMINDERS -> {
                printReminders();
                yield false;
            }
            case CANCEL -> {
                handleCancellation(command.number());
                yield false;
            }
            case UNKNOWN -> {
                ui.println("I did not understand that. Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
                yield false;
            }
            };
        } catch (IllegalArgumentException exception) {
            ui.println("I could not create that plan: " + exception.getMessage());
            return false;
        }
    }

    private void handlePlan(PlanCommand plan) {
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
    }

    private List<RouteAlternative> findAlternatives(PlanCommand plan) {
        var result = planner.findAlternatives(plan);
        result.messages().forEach(ui::println);
        ui.println();
        return result.alternatives();
    }

    private void handleChoice(Integer routeNumber) {
        if (pendingPlan == null) {
            ui.println("Please create a plan before choosing a route.");
            return;
        }
        if (routeNumber == null) {
            ui.println("Choose a route by number, for example: choose 1");
            return;
        }
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

    private void handleCancellation(Integer reminderNumber) {
        if (reminderNumber == null) {
            ui.println("Cancel a reminder by number, for example: cancel 1");
            return;
        }
        if (departureReminderService.cancel(reminderNumber)) {
            ui.println("Cancelled departure reminder " + reminderNumber + ".");
        } else {
            ui.println("No active departure reminder numbered " + reminderNumber + ".");
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
