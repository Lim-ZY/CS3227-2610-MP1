package Timey.ui.dashboard;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import Timey.ApplicationFactory;
import Timey.ui.CommandLineApp;
import Timey.ui.CommandExecutionResult;
import Timey.ui.DashboardState;
import Timey.ui.Ui;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

/** JavaFX presentation shell for Timey's dashboard. */
public final class TimeyDashboardApp extends Application {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard");
        Header header = createHeader();
        root.setTop(header.container());
        StringWriter output = new StringWriter();
        CommandLineApp commandLineApp = ApplicationFactory.createCommandLineApp(
                new Ui(new BufferedReader(new StringReader("")), new PrintWriter(output, true)));
        TextArea commandOutput = createCommandOutput();
        DashboardContent dashboard = createDashboard(commandOutput);
        root.setCenter(dashboard.content());
        root.setBottom(createCommandBar(commandLineApp, output, commandOutput, dashboard, header));

        Scene scene = new Scene(root, 1120, 760);
        scene.getStylesheets().add(getClass().getResource("/Timey/ui/dashboard/dashboard.css").toExternalForm());
        stage.setTitle("Timey");
        stage.setMinWidth(880);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    private Header createHeader() {
        MenuButton timey = new MenuButton("Timey");
        timey.getStyleClass().add("brand");
        MenuItem savedLocationStatus = new MenuItem("No saved locations yet");
        Menu savedLocations = new Menu("Saved locations");
        savedLocations.getItems().add(savedLocationStatus);
        MenuItem recentLocations = new MenuItem("No recent plan");
        Menu recent = new Menu("Recent locations");
        recent.getItems().add(recentLocations);
        MenuItem personalBuffer = new MenuItem("Set per plan");
        MenuItem timeZone = new MenuItem("Asia/Singapore");
        Menu preferences = new Menu("Preferences");
        preferences.getItems().addAll(new Menu("Personal buffer", null, personalBuffer),
                new Menu("Time zone", null, timeZone));
        timey.getItems().addAll(savedLocations, recent, preferences);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(16, timey, spacer, new Label("Dashboard"));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return new Header(header, recentLocations, personalBuffer);
    }

    private DashboardContent createDashboard(TextArea commandOutput) {
        Label heading = new Label("Your day, on track.");
        heading.getStyleClass().add("page-heading");
        Label introduction = new Label("Plan a commute in the command bar to see your next event and departure plan here.");
        introduction.getStyleClass().add("muted");

        Card nextEvent = card("NEXT EVENT", "No commute planned", "Your route, departure time, and reminder will appear here.");
        Card commute = card("COMMUTE STATUS", "Waiting for a plan", "Live rail alternatives are requested only after you plan a commute.");
        Card reminders = card("REMINDER STATUS", "No active reminders", "Timey will automatically schedule a departure reminder after you choose a route.");
        HBox lowerCards = new HBox(18, commute.container(), reminders.container());
        lowerCards.getChildren().forEach(card -> HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS));
        VBox alternatives = createAlternativesPanel();

        Label commandHeading = new Label("COMMAND OUTPUT");
        commandHeading.getStyleClass().add("card-label");
        VBox content = new VBox(18, heading, introduction, nextEvent.container(), lowerCards, alternatives,
                commandHeading, commandOutput);
        content.setPadding(new Insets(40, 56, 32, 56));
        return new DashboardContent(content, nextEvent, commute, reminders, alternatives);
    }

    private Card card(String label, String title, String message) {
        Label cardLabel = new Label(label);
        cardLabel.getStyleClass().add("card-label");
        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("card-title");
        Label cardMessage = new Label(message);
        cardMessage.getStyleClass().add("muted");
        cardMessage.setWrapText(true);
        VBox card = new VBox(10, cardLabel, cardTitle, cardMessage);
        card.getStyleClass().add("card");
        card.setMinHeight(136);
        return new Card(card, cardTitle, cardMessage);
    }

    private TextArea createCommandOutput() {
        TextArea commandOutput = new TextArea("Use the command bar below, for example:\n"
                + "plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");
        commandOutput.setEditable(false);
        commandOutput.setWrapText(true);
        commandOutput.setPrefRowCount(8);
        commandOutput.getStyleClass().add("command-output");
        return commandOutput;
    }

    private VBox createAlternativesPanel() {
        Label heading = new Label("ROUTE ALTERNATIVES");
        heading.getStyleClass().add("card-label");
        Label guidance = new Label("Plan a commute to compare routes. Select one from the command bar with choose <number>.");
        guidance.getStyleClass().add("muted");
        VBox alternatives = new VBox(12, heading, guidance);
        alternatives.getStyleClass().add("card");
        return alternatives;
    }

    private HBox createCommandBar(CommandLineApp commandLineApp, StringWriter output, TextArea commandOutput,
            DashboardContent dashboard, Header header) {
        Label prompt = new Label(">");
        prompt.getStyleClass().add("command-prompt");
        TextField command = new TextField();
        command.setPromptText("Enter a Timey command, for example: plan /from \"COM3\" /to \"VivoCity\" /by 1830");
        command.setOnAction(event -> executeCommand(commandLineApp, output, commandOutput, command, dashboard, header));
        HBox.setHgrow(command, javafx.scene.layout.Priority.ALWAYS);
        HBox commandBar = new HBox(12, prompt, command);
        commandBar.setAlignment(Pos.CENTER_LEFT);
        commandBar.setPadding(new Insets(18, 56, 24, 56));
        commandBar.getStyleClass().add("command-bar");
        return commandBar;
    }

    private void executeCommand(CommandLineApp commandLineApp, StringWriter output, TextArea commandOutput,
            TextField command, DashboardContent dashboard, Header header) {
        String input = command.getText();
        if (input.isBlank()) {
            return;
        }
        command.clear();
        command.setDisable(true);
        command.setPromptText("Updating your commute…");
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
            commandOutput.appendText("\n> " + input + "\n" + response.output());
            refreshDashboard(dashboard, header, response.result().dashboardState());
            if (response.result().sessionEnded()) {
                command.setPromptText("This command session has ended");
            } else {
                command.setDisable(false);
                command.setPromptText("Enter a Timey command, for example: choose 1");
            }
        });
        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            commandOutput.appendText("\n> " + input + "\nI could not complete that command. Please try again.");
            showFailure(dashboard, failure);
            command.setDisable(false);
            command.setPromptText("Enter a Timey command, for example: plan /from \"COM3\" /to \"VivoCity\" /by 1830");
        });
        Thread worker = new Thread(task, "timey-dashboard-command");
        worker.setDaemon(true);
        worker.start();
    }

    private void refreshDashboard(DashboardContent dashboard, Header header, DashboardState state) {
        state.plan().ifPresentOrElse(plan -> {
            dashboard.nextEvent().title().setText("Physical · " + plan.origin() + " → " + plan.destination());
            dashboard.nextEvent().message().setText("Arrive by " + TIME_FORMAT.format(plan.arrivalTime())
                    + " · " + plan.buffer().toMinutes() + " minute buffer");
        }, () -> {
            dashboard.nextEvent().title().setText("No commute planned");
            dashboard.nextEvent().message().setText("Your route, departure time, and reminder will appear here.");
        });
        state.recommendation().ifPresentOrElse(recommendation -> {
            dashboard.commute().title().setText("Leave by " + TIME_FORMAT.format(recommendation.departureTime()));
            dashboard.commute().message().setText(recommendation.routeName() + " · "
                    + recommendation.travelDuration().toMinutes() + " minute commute");
        }, () -> {
            DashboardCommuteStatus status = DashboardCommuteStatus.from(state);
            dashboard.commute().title().setText(status.title());
            dashboard.commute().message().setText(status.message());
        });
        dashboard.reminders().title().setText(state.reminders().isEmpty() ? "No active reminders"
                : state.reminders().size() + " active departure reminder");
        dashboard.reminders().message().setText(state.reminders().isEmpty()
                ? "Timey will automatically schedule a departure reminder after you choose a route."
                : "Next reminder: " + state.reminders().getFirst().triggerAt().atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")));
        refreshAlternatives(dashboard.alternatives(), state);
        header.recentLocations().setText(DashboardMenuSummary.recentLocations(state));
        header.personalBuffer().setText(DashboardMenuSummary.personalBuffer(state));
    }

    private void showLoading(DashboardContent dashboard) {
        dashboard.commute().title().setText("Updating commute…");
        dashboard.commute().message().setText("Looking up locations and live rail alternatives. Please wait.");
        dashboard.alternatives().getChildren().setAll(routePanelLabel("ROUTE ALTERNATIVES"),
                routePanelMessage("Loading route alternatives…"));
    }

    private void showFailure(DashboardContent dashboard, Throwable failure) {
        dashboard.commute().title().setText("Could not update commute");
        dashboard.commute().message().setText("Your previous plan is unchanged. "
                + (failure.getMessage() == null ? "Please try again." : failure.getMessage()));
        dashboard.alternatives().getChildren().setAll(routePanelLabel("ROUTE ALTERNATIVES"),
                routePanelMessage("Route lookup failed. Try the command again."));
    }

    private void refreshAlternatives(VBox alternatives, DashboardState state) {
        alternatives.getChildren().setAll();
        alternatives.getChildren().add(routePanelLabel("ROUTE ALTERNATIVES"));
        if (state.alternatives().isEmpty()) {
            alternatives.getChildren().add(routePanelMessage(
                    "Plan a commute to compare routes. Select one from the command bar with choose <number>."));
            return;
        }
        for (int index = 0; index < state.alternatives().size(); index++) {
            var route = state.alternatives().get(index);
            boolean selected = state.recommendation().map(recommendation -> recommendation.routeName().equals(route.name()))
                    .orElse(false);
            alternatives.getChildren().add(routeAlternative(index + 1, route, selected));
        }
        Label guidance = new Label("Select a route from the command bar, for example: choose 1");
        guidance.getStyleClass().add("route-guidance");
        alternatives.getChildren().add(guidance);
    }

    private VBox routeAlternative(int routeNumber, Timey.domain.transit.RouteAlternative route, boolean selected) {
        Label name = new Label(routeNumber + ". " + route.name() + " · " + route.totalDuration().toMinutes() + " min"
                + (selected ? "  SELECTED" : ""));
        name.getStyleClass().add(selected ? "route-name-selected" : "route-name");
        Label details = new Label("Walk " + route.walkingDuration().toMinutes() + " min · Rail "
                + route.transitDuration().toMinutes() + " min · " + route.transferCount()
                + (route.transferCount() == 1 ? " transfer" : " transfers"));
        details.getStyleClass().add("muted");
        VBox alternative = new VBox(5, name, details);
        alternative.getStyleClass().add(selected ? "route-alternative-selected" : "route-alternative");
        return alternative;
    }

    private Label routePanelLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-label");
        return label;
    }

    private Label routePanelMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }

    private record Card(VBox container, Label title, Label message) {
    }

    private record DashboardContent(VBox content, Card nextEvent, Card commute, Card reminders, VBox alternatives) {
    }

    private record Header(HBox container, MenuItem recentLocations, MenuItem personalBuffer) {
    }

    private record DashboardCommandResponse(CommandExecutionResult result, String output) {
    }
}
