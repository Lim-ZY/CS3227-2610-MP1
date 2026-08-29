package timey.ui.dashboard;

import java.io.StringWriter;
import java.util.Objects;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import timey.config.ApplicationConfiguration;
import timey.config.UserPreferences;
import timey.ui.CommandExecutionResult;
import timey.ui.CommandLineApp;
import timey.ui.DashboardState;
import timey.ui.UiPart;

/** Main FXML-backed window that hosts and coordinates Timey's dashboard UI parts. */
public final class MainWindow extends UiPart<Stage> {
    private static final String FXML = "MainWindow.fxml";

    @FXML
    private StackPane headerPlaceholder;
    @FXML
    private StackPane dashboardPlaceholder;
    @FXML
    private StackPane commandBarPlaceholder;

    private final CommandLineApp commandLineApp;
    private final StringWriter output;
    private final DashboardHeader header;
    private final CommandOutput commandOutput;
    private final CommandBar commandBar;
    private final DashboardContent dashboard;

    /** Creates a new MainWindow. */
    public MainWindow(Stage primaryStage, CommandLineApp commandLineApp, StringWriter output,
            UserPreferences userPreferences) {
        super(FXML, primaryStage);
        this.commandLineApp = Objects.requireNonNull(commandLineApp);
        this.output = Objects.requireNonNull(output);
        this.header = new DashboardHeader(Objects.requireNonNull(userPreferences));
        this.commandOutput = new CommandOutput();
        this.commandBar = new CommandBar();
        this.dashboard = createDashboard();
        commandBar.setCommandExecutor(this::executeCommand);
        fillInnerParts();
        getRoot().setOnHidden(event -> {
            commandLineApp.close();
            header.stopClock();
        });
    }

    /** Displays the window and starts the header clock. */
    public void show() {
        getRoot().show();
        header.startClock(ApplicationConfiguration.TIME_ZONE);
    }

    private void fillInnerParts() {
        headerPlaceholder.getChildren().setAll(header.getRoot());
        dashboardPlaceholder.getChildren().setAll(dashboard.content());
        commandBarPlaceholder.getChildren().setAll(commandBar.getRoot());
    }

    private DashboardContent createDashboard() {
        Label heading = new Label("Your day, on track.");
        heading.getStyleClass().add("page-heading");
        Label introduction = new Label(
                "Plan a commute in the command bar to see your next event and departure plan here.");
        introduction.getStyleClass().add("muted");

        NextEventCard nextEvent = new NextEventCard();
        CommuteStatusCard commute = new CommuteStatusCard();
        ReminderStatusCard reminders = new ReminderStatusCard();
        HBox lowerCards = new HBox(18, commute.getRoot(), reminders.getRoot());
        lowerCards.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        RouteAlternativesPanel alternatives = new RouteAlternativesPanel();

        Label commandHeading = new Label("COMMAND OUTPUT");
        commandHeading.getStyleClass().add("card-label");
        VBox content = new VBox(18, heading, introduction, nextEvent.getRoot(), lowerCards, alternatives.getRoot(),
                commandHeading, commandOutput.getRoot());
        content.setPadding(new Insets(40, 56, 32, 56));
        return new DashboardContent(content, nextEvent, commute, reminders, alternatives);
    }

    private void executeCommand(String input) {
        showLoading();
        Task<DashboardCommandResponse> task = new Task<>() {
            @Override
            protected DashboardCommandResponse call() {
                int outputStart = output.getBuffer().length();
                CommandExecutionResult result = commandLineApp.executeCommand(input);
                return new DashboardCommandResponse(result, output.getBuffer().substring(outputStart));
            }
        };
        task.setOnSucceeded(event -> handleCommandSuccess(input, task.getValue()));
        task.setOnFailed(event -> handleCommandFailure(input, task.getException()));
        Thread worker = new Thread(task, "timey-dashboard-command");
        worker.setDaemon(true);
        worker.start();
    }

    private void handleCommandSuccess(String input, DashboardCommandResponse response) {
        commandOutput.appendCommandResult(input, response.output());
        refreshDashboard(response.result().dashboardState());
        if (response.result().sessionEnded()) {
            commandBar.showSessionEnded();
        } else {
            commandBar.showReadyAfterSuccess();
        }
    }

    private void handleCommandFailure(String input, Throwable failure) {
        commandOutput.appendCommandFailure(input);
        dashboard.commute().showFailure(failure);
        dashboard.alternatives().showFailure();
        commandBar.showReadyAfterFailure();
    }

    private void refreshDashboard(DashboardState state) {
        dashboard.nextEvent().render(state);
        dashboard.commute().render(state);
        dashboard.reminders().render(state);
        dashboard.alternatives().render(state);
        header.refresh(state);
    }

    private void showLoading() {
        dashboard.commute().showLoading();
        dashboard.alternatives().showLoading();
    }

    private record DashboardContent(VBox content, NextEventCard nextEvent, CommuteStatusCard commute,
            ReminderStatusCard reminders, RouteAlternativesPanel alternatives) {
    }

    private record DashboardCommandResponse(CommandExecutionResult result, String output) {
    }
}
