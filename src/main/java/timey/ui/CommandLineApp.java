package timey.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.util.List;

import timey.command.Command;
import timey.command.CommandResult;
import timey.config.ApplicationConfiguration;
import timey.domain.transit.LiveRouteLookup;
import timey.infrastructure.http.HttpResult;
import timey.infrastructure.location.OneMapLocationResolver;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;
import timey.infrastructure.transit.MockTransitPlanner;
import timey.model.TimeyModel;
import timey.parser.Parser;
import timey.parser.PlanCommandParser;
import timey.planner.CommutePlanningService;
import timey.ports.FixedCommuteStore;
import timey.ports.LocationResolver;
import timey.ports.PlanStore;
import timey.ports.RailTransitPlanner;
import timey.ports.ReminderScheduler;

/** Interactive command-line presentation for timey. */
public final class CommandLineApp {
    private final ConsoleUi ui;
    private final Parser parser;
    private final TimeyModel model;

    /** Creates a new CommandLineApp. */
    public CommandLineApp(BufferedReader input, PrintWriter output) {
        this(input, output, new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()),
                new OneMapLocationResolver(uri -> new HttpResult(503, ""), java.util.Optional.empty()));
    }

    /** Creates a new CommandLineApp. */
    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver) {
        this(new ConsoleUi(input, output), planCommandParser, commutePlanningService, locationResolver,
                noLiveRoutes(), Clock.system(ApplicationConfiguration.TIME_ZONE), noOpReminderScheduler(),
                new InMemoryFixedCommuteStore());
    }

    /** Creates a new CommandLineApp. */
    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock) {
        this(new ConsoleUi(input, output), planCommandParser, commutePlanningService, locationResolver,
                railTransitPlanner, clock, noOpReminderScheduler());
    }

    /** Creates a new CommandLineApp. */
    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler) {
        this(new ConsoleUi(input, output), planCommandParser, commutePlanningService, locationResolver,
                railTransitPlanner, clock,
                reminderScheduler);
    }

    /** Creates a new CommandLineApp. */
    public CommandLineApp(BufferedReader input, PrintWriter output, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler,
            FixedCommuteStore fixedCommuteStore) {
        this(new ConsoleUi(input, output), planCommandParser, commutePlanningService, locationResolver,
                railTransitPlanner, clock,
                reminderScheduler, fixedCommuteStore);
    }

    /** Creates a new CommandLineApp. */
    public CommandLineApp(ConsoleUi ui, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler) {
        this(ui, planCommandParser, commutePlanningService, locationResolver, railTransitPlanner, clock,
                reminderScheduler, new InMemoryFixedCommuteStore());
    }

    /** Creates a new CommandLineApp. */
    public CommandLineApp(ConsoleUi ui, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler,
            FixedCommuteStore fixedCommuteStore) {
        this(ui, planCommandParser, commutePlanningService, locationResolver, railTransitPlanner, clock,
                reminderScheduler, fixedCommuteStore, plans -> { });
    }

    /** Creates a new CommandLineApp. */
    public CommandLineApp(ConsoleUi ui, PlanCommandParser planCommandParser,
            CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock, ReminderScheduler reminderScheduler,
            FixedCommuteStore fixedCommuteStore, PlanStore planStore) {
        this.ui = ui;
        this.parser = new Parser(planCommandParser);
        var planner = new timey.planner.Planner(commutePlanningService, locationResolver, railTransitPlanner, clock);
        var departureReminderService = new timey.reminder.DepartureReminderService(reminderScheduler, clock,
                reminder -> {
                    ui.println();
                    ui.println(reminder.message());
                    ui.printPrompt();
                });
        this.model = new TimeyModel(planner, fixedCommuteStore, planStore, departureReminderService, clock);
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
        } finally {
            close();
        }
    }

    /** Prunes expired saved plans before the command session ends. */
    public void close() {
        model.close();
    }

    /** Executes one command while retaining the same command state as the terminal session. */
    public CommandExecutionResult executeCommand(String input) {
        boolean sessionEnded;
        try {
            Command command = parser.parse(input);
            assert command != null : "Parser.parse should return a Command";
            execute(command);
            sessionEnded = command.isExit();
        } catch (IllegalArgumentException exception) {
            ui.println("I could not create that plan: " + exception.getMessage());
            sessionEnded = false;
        } catch (RuntimeException exception) {
            ui.println("Timey could not complete that command. Please try again.");
            sessionEnded = false;
        }
        return new CommandExecutionResult(sessionEnded, getDashboardState());
    }

    private CommandResult execute(Command command) {
        CommandResult result = command.execute(model);
        ui.show(result);
        return result;
    }

    private static RailTransitPlanner noLiveRoutes() {
        return (origin, destination, date, time) -> LiveRouteLookup.available(List.of());
    }

    private static ReminderScheduler noOpReminderScheduler() {
        return (triggerAt, action) -> () -> { };
    }

    /** Returns current session data for a dashboard without invoking any planner or API itself. */
    public DashboardState getDashboardState() {
        return new DashboardState(model.getPendingPlan(), model.getPendingAlternatives(), model.getPlanningMessages(),
                model.getSelectedRecommendation(), model.getScheduledReminders());
    }
}
