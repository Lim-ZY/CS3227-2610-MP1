package timey.ui.dashboard;

import java.io.StringWriter;
import java.util.Objects;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
    private static final javafx.util.Duration DASHBOARD_REFRESH_INTERVAL = javafx.util.Duration.seconds(30);

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
    private final DashboardCommandTracker commandTracker = new DashboardCommandTracker();
    private Task<DashboardCommandResponse> activeCommandTask;
    private DashboardState latestDashboardState;
    private Timeline dashboardRefreshTimer;

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
            commandTracker.close();
            cancelActiveCommand();
            commandLineApp.close();
            header.stopClock();
            stopDashboardRefresh();
        });
    }

    /** Displays the window and starts the header clock. */
    public void show() {
        getRoot().show();
        refreshDashboard(commandLineApp.getDashboardState());
        header.startClock(ApplicationConfiguration.TIME_ZONE);
        startDashboardRefresh();
    }

    private void fillInnerParts() {
        headerPlaceholder.getChildren().setAll(header.getRoot());
        dashboardPlaceholder.getChildren().setAll(dashboard.content());
        commandBarPlaceholder.getChildren().setAll(commandBar.getRoot());
    }

    private DashboardContent createDashboard() {
        NextEventCard nextEvent = new NextEventCard();
        CommuteStatusCard commute = new CommuteStatusCard();
        ReminderStatusCard reminders = new ReminderStatusCard();
        RouteAlternativesPanel alternatives = new RouteAlternativesPanel();
        VBox content = createDashboardContent(nextEvent, commute, reminders, alternatives);
        return new DashboardContent(content, nextEvent, commute, reminders, alternatives);
    }

    private VBox createDashboardContent(NextEventCard nextEvent, CommuteStatusCard commute,
            ReminderStatusCard reminders, RouteAlternativesPanel alternatives) {
        Label heading = new Label("Your day, on track.");
        heading.getStyleClass().add("page-heading");
        Label introduction = new Label(
                "Plan a commute in the command bar to see your next event and departure plan here.");
        introduction.getStyleClass().add("muted");

        Label commandHeading = new Label("COMMAND OUTPUT");
        commandHeading.getStyleClass().add("card-label");
        VBox content = new VBox(18, heading, introduction, nextEvent.getRoot(), createLowerCards(commute, reminders),
                alternatives.getRoot(),
                commandHeading, commandOutput.getRoot());
        content.setPadding(new Insets(40, 56, 32, 56));
        return content;
    }

    private HBox createLowerCards(CommuteStatusCard commute, ReminderStatusCard reminders) {
        HBox lowerCards = new HBox(18, commute.getRoot(), reminders.getRoot());
        lowerCards.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        return lowerCards;
    }

    private void executeCommand(String input) {
        long requestId = commandTracker.startRequest();
        cancelActiveCommand();
        showLoading();
        Task<DashboardCommandResponse> task = new Task<>() {
            @Override
            protected DashboardCommandResponse call() {
                int outputStart = output.getBuffer().length();
                CommandExecutionResult result = commandLineApp.executeCommand(input);
                return new DashboardCommandResponse(result, output.getBuffer().substring(outputStart));
            }
        };
        task.setOnSucceeded(event -> handleCommandSuccess(requestId, input, task.getValue()));
        task.setOnFailed(event -> handleCommandFailure(requestId, input, task.getException()));
        activeCommandTask = task;
        Thread worker = new Thread(task, "timey-dashboard-command");
        worker.setDaemon(true);
        worker.start();
    }

    private void handleCommandSuccess(long requestId, String input, DashboardCommandResponse response) {
        if (!commandTracker.isCurrent(requestId)) {
            return;
        }
        activeCommandTask = null;
        commandOutput.appendCommandResult(input, response.output());
        refreshDashboard(response.result().dashboardState());
        if (response.result().sessionEnded()) {
            commandBar.showSessionEnded();
        } else {
            commandBar.showReadyAfterSuccess();
        }
    }

    private void handleCommandFailure(long requestId, String input, Throwable failure) {
        if (!commandTracker.isCurrent(requestId)) {
            return;
        }
        activeCommandTask = null;
        commandOutput.appendCommandFailure(input);
        if (latestDashboardState != null) {
            refreshDashboard(latestDashboardState);
        } else {
            dashboard.alternatives().showFailure();
        }
        dashboard.commute().showFailure(failure);
        commandBar.showReadyAfterFailure();
    }

    private void refreshDashboard(DashboardState state) {
        latestDashboardState = Objects.requireNonNull(state);
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

    private void startDashboardRefresh() {
        stopDashboardRefresh();
        dashboardRefreshTimer = new Timeline(new KeyFrame(DASHBOARD_REFRESH_INTERVAL,
                event -> refreshNextEventCard()));
        dashboardRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        dashboardRefreshTimer.play();
    }

    private void stopDashboardRefresh() {
        if (dashboardRefreshTimer != null) {
            dashboardRefreshTimer.stop();
        }
    }

    private void refreshNextEventCard() {
        if (latestDashboardState != null) {
            dashboard.nextEvent().render(latestDashboardState);
        }
    }

    private void cancelActiveCommand() {
        if (activeCommandTask != null) {
            activeCommandTask.cancel();
            activeCommandTask = null;
        }
    }

    private record DashboardContent(VBox content, NextEventCard nextEvent, CommuteStatusCard commute,
            ReminderStatusCard reminders, RouteAlternativesPanel alternatives) {
    }

    private record DashboardCommandResponse(CommandExecutionResult result, String output) {
    }
}
