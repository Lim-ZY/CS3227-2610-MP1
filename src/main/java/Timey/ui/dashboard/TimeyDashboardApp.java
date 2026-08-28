package Timey.ui.dashboard;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import Timey.ApplicationFactory;
import Timey.config.ApplicationConfiguration;
import Timey.ui.CommandLineApp;
import Timey.ui.CommandExecutionResult;
import Timey.ui.DashboardState;
import Timey.ui.ConsoleUi;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** JavaFX presentation shell for Timey's dashboard. */
public final class TimeyDashboardApp extends Application {
    @Override
    public void start(Stage stage) {
        DashboardHeader header = new DashboardHeader(ApplicationFactory.loadUserPreferences());
        StringWriter output = new StringWriter();
        CommandLineApp commandLineApp = ApplicationFactory.createCommandLineApp(
                new ConsoleUi(new BufferedReader(new StringReader("")), new PrintWriter(output, true)));
        CommandOutput commandOutput = new CommandOutput();
        DashboardContent dashboard = createDashboard(commandOutput);
        CommandBar commandBar = new CommandBar();
        commandBar.setCommandExecutor(input -> executeCommand(commandLineApp, output, commandOutput, commandBar,
                input, dashboard, header));
        MainWindow mainWindow = new MainWindow(stage);
        mainWindow.setHeader(header.getRoot());
        mainWindow.setDashboardContent(dashboard.content());
        mainWindow.setCommandBar(commandBar.getRoot());
        mainWindow.show();
        header.startClock(ApplicationConfiguration.TIME_ZONE);
        mainWindow.setOnHidden(event -> header.stopClock());
    }

    private DashboardContent createDashboard(CommandOutput commandOutput) {
        Label heading = new Label("Your day, on track.");
        heading.getStyleClass().add("page-heading");
        Label introduction = new Label("Plan a commute in the command bar to see your next event and departure plan here.");
        introduction.getStyleClass().add("muted");

        NextEventCard nextEvent = new NextEventCard();
        CommuteStatusCard commute = new CommuteStatusCard();
        ReminderStatusCard reminders = new ReminderStatusCard();
        HBox lowerCards = new HBox(18, commute.getRoot(), reminders.getRoot());
        lowerCards.getChildren().forEach(card -> HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS));
        RouteAlternativesPanel alternatives = new RouteAlternativesPanel();

        Label commandHeading = new Label("COMMAND OUTPUT");
        commandHeading.getStyleClass().add("card-label");
        VBox content = new VBox(18, heading, introduction, nextEvent.getRoot(), lowerCards, alternatives.getRoot(),
                commandHeading, commandOutput.getRoot());
        content.setPadding(new Insets(40, 56, 32, 56));
        return new DashboardContent(content, nextEvent, commute, reminders, alternatives);
    }

    private void executeCommand(CommandLineApp commandLineApp, StringWriter output, CommandOutput commandOutput,
            CommandBar commandBar, String input, DashboardContent dashboard, DashboardHeader header) {
        showLoading(dashboard);
        Task<DashboardCommandResponse> task = new Task<>() {
            @Override
            protected DashboardCommandResponse call() {
                int outputStart = output.getBuffer().length();
                CommandExecutionResult result = commandLineApp.executeCommand(input);
                return new DashboardCommandResponse(result, output.getBuffer().substring(outputStart));
            }
        };
        task.setOnSucceeded(event -> {
            DashboardCommandResponse response = task.getValue();
            commandOutput.appendCommandResult(input, response.output());
            refreshDashboard(dashboard, header, response.result().dashboardState());
            if (response.result().sessionEnded()) {
                commandBar.showSessionEnded();
            } else {
                commandBar.showReadyAfterSuccess();
            }
        });
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            commandOutput.appendCommandFailure(input);
            showFailure(dashboard, failure);
            commandBar.showReadyAfterFailure();
        });
        Thread worker = new Thread(task, "timey-dashboard-command");
        worker.setDaemon(true);
        worker.start();
    }

    private void refreshDashboard(DashboardContent dashboard, DashboardHeader header, DashboardState state) {
        dashboard.nextEvent().render(state);
        dashboard.commute().render(state);
        dashboard.reminders().render(state);
        dashboard.alternatives().render(state);
        header.refresh(state);
    }

    private void showLoading(DashboardContent dashboard) {
        dashboard.commute().showLoading();
        dashboard.alternatives().showLoading();
    }

    private void showFailure(DashboardContent dashboard, Throwable failure) {
        dashboard.commute().showFailure(failure);
        dashboard.alternatives().showFailure();
    }

    private record DashboardContent(VBox content, NextEventCard nextEvent, CommuteStatusCard commute,
            ReminderStatusCard reminders,
            RouteAlternativesPanel alternatives) {
    }

    private record DashboardCommandResponse(CommandExecutionResult result, String output) {
    }
}
