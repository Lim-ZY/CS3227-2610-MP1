package Timey.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Timey.reminder.DepartureReminderService;
import Timey.command.Command;
import Timey.command.CancelCommand;
import Timey.command.CommandResult;
import Timey.command.ThanksCommand;
import Timey.command.AddCommand;
import Timey.command.UnknownCommand;
import Timey.model.TimeyModel;
import Timey.planner.CommutePlanningService;
import Timey.parser.Parser;
import Timey.command.PlanCommand;
import Timey.command.RemindersCommand;
import Timey.parser.PlanCommandParser;
import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.transit.LiveRouteLookup;
import Timey.domain.transit.RouteAlternative;
import Timey.infrastructure.http.HttpResult;
import Timey.infrastructure.location.OneMapLocationResolver;
import Timey.infrastructure.transit.InMemoryFixedCommuteStore;
import Timey.infrastructure.transit.MockTransitPlanner;
import Timey.ports.LocationResolver;
import Timey.ports.FixedCommuteStore;
import Timey.ports.RailTransitPlanner;
import Timey.ports.ReminderScheduler;

/** Interactive command-line presentation for Timey. */
public final class CommandLineApp {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter REMINDER_TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    private final Ui ui;
    private final Parser parser;
    private final CommutePlanningService commutePlanningService;
    private final DepartureReminderService departureReminderService;
    private final Clock clock;
    private final TimeyModel model;

    public CommandLineApp(BufferedReader input, PrintWriter output) {
        this(input, output, new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()),
                new OneMapLocationResolver((uri, authorization) -> new HttpResult(503, ""),
                        java.util.Optional.empty()));
    }

    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver) {
        this(new Ui(input, output), planCommandParser, commutePlanningService, locationResolver,
                (origin, destination, date, time) -> LiveRouteLookup.available(List.of()),
                Clock.systemDefaultZone(), (triggerAt, action) -> () -> { }, new InMemoryFixedCommuteStore());
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

    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler,
            FixedCommuteStore fixedCommuteStore) {
        this(new Ui(input, output), planCommandParser, commutePlanningService, locationResolver, railTransitPlanner, clock,
                reminderScheduler, fixedCommuteStore);
    }

    public CommandLineApp(Ui ui, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler) {
        this(ui, planCommandParser, commutePlanningService, locationResolver, railTransitPlanner, clock,
                reminderScheduler, new InMemoryFixedCommuteStore());
    }

    public CommandLineApp(Ui ui, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler,
            FixedCommuteStore fixedCommuteStore) {
        this.ui = ui;
        this.parser = new Parser(planCommandParser);
        this.commutePlanningService = commutePlanningService;
        var planner = new Timey.planner.Planner(commutePlanningService, locationResolver, railTransitPlanner, clock);
        this.departureReminderService = new DepartureReminderService(reminderScheduler, clock);
        this.clock = clock;
        this.model = new TimeyModel(planner, fixedCommuteStore, departureReminderService, clock);
    }

    /** Runs until the user says thanks or standard input closes. */
    public void run() {
        ui.printWelcome();
        try {
            String command;
            while ((command = ui.readCommand()) != null) {
                ui.printDivider();
                if (executeCommand(command).sessionEnded()) {
                    return;
                }
                ui.printDivider();
                ui.printPrompt();
            }
        } catch (IOException exception) {
            ui.showReadingError();
        }
    }

    /** Executes one command while retaining the same command state as the terminal session. */
    public CommandExecutionResult executeCommand(String input) {
        boolean sessionEnded;
        try {
            Parser.ParsedCommand command = parser.parse(input);
            sessionEnded = switch (command.type()) {
            case THANKS -> {
                Command thanksCommand = new ThanksCommand();
                execute(thanksCommand);
                yield thanksCommand.isExit();
            }
            case PLAN -> {
                PlanCommand planCommand = command.plan();
                execute(planCommand);
                yield planCommand.isExit();
            }
            case ADD -> {
                AddCommand addCommand = command.addCommand();
                execute(addCommand);
                yield addCommand.isExit();
            }
            case CHOOSE -> {
                handleChoice(command.number());
                yield false;
            }
            case REMINDERS -> {
                Command remindersCommand = new RemindersCommand();
                execute(remindersCommand);
                yield remindersCommand.isExit();
            }
            case CANCEL -> {
                Command cancelCommand = new CancelCommand(command.number());
                execute(cancelCommand);
                yield cancelCommand.isExit();
            }
            case UNKNOWN -> {
                UnknownCommand unknownCommand = new UnknownCommand();
                execute(unknownCommand);
                yield unknownCommand.isExit();
            }
            };
        } catch (IllegalArgumentException exception) {
            ui.println("I could not create that plan: " + exception.getMessage());
            sessionEnded = false;
        }
        return new CommandExecutionResult(sessionEnded, getDashboardState());
    }

    private CommandResult execute(Command command) {
        CommandResult result = command.execute(model);
        ui.show(result);
        return result;
    }

    /** Returns current session data for a dashboard without invoking any planner or API itself. */
    public DashboardState getDashboardState() {
        return new DashboardState(model.getPendingPlan(), model.getPendingAlternatives(), model.getPlanningMessages(),
                model.getSelectedRecommendation(), model.getScheduledReminders());
    }


    private void handleChoice(Integer routeNumber) {
        if (model.getPendingPlan().isEmpty()) {
            ui.println("Please create a plan before choosing a route.");
            return;
        }
        if (routeNumber == null) {
            ui.println("Choose a route by number, for example: choose 1");
            return;
        }
        if (routeNumber < 1 || routeNumber > model.getPendingAlternatives().size()) {
            ui.println("Please choose a route between 1 and " + model.getPendingAlternatives().size() + ".");
            return;
        }
        RouteAlternative route = model.getPendingAlternatives().get(routeNumber - 1);
        DepartureRecommendation recommendation = commutePlanningService.recommendDeparture(
                model.getPendingPlan().orElseThrow(), route);
        model.selectRecommendation(recommendation);
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
