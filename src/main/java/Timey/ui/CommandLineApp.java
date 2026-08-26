package Timey.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.util.List;

import Timey.command.Command;
import Timey.command.CancelCommand;
import Timey.command.ChooseCommand;
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
import Timey.domain.transit.LiveRouteLookup;
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
    private final Ui ui;
    private final Parser parser;
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
        var planner = new Timey.planner.Planner(commutePlanningService, locationResolver, railTransitPlanner, clock);
        var departureReminderService = new Timey.reminder.DepartureReminderService(reminderScheduler, clock, reminder -> {
            ui.println();
            ui.println(reminder.message());
            ui.printPrompt();
        });
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
                Command chooseCommand = new ChooseCommand(command.number());
                execute(chooseCommand);
                yield chooseCommand.isExit();
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
}
