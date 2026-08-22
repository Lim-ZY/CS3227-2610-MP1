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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
        root.setTop(createHeader());
        StringWriter output = new StringWriter();
        CommandLineApp commandLineApp = ApplicationFactory.createCommandLineApp(
                new Ui(new BufferedReader(new StringReader("")), new PrintWriter(output, true)));
        TextArea commandOutput = createCommandOutput();
        DashboardContent dashboard = createDashboard(commandOutput);
        root.setCenter(dashboard.content());
        root.setBottom(createCommandBar(commandLineApp, output, commandOutput, dashboard));

        Scene scene = new Scene(root, 1120, 760);
        scene.getStylesheets().add(getClass().getResource("/Timey/ui/dashboard/dashboard.css").toExternalForm());
        stage.setTitle("Timey");
        stage.setMinWidth(880);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    private HBox createHeader() {
        Label timey = new Label("Timey");
        timey.getStyleClass().add("brand");
        Button preferences = new Button("Preferences ▾");
        preferences.getStyleClass().add("preferences-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(16, timey, preferences, spacer, new Label("Dashboard"));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return header;
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

        Label commandHeading = new Label("COMMAND OUTPUT");
        commandHeading.getStyleClass().add("card-label");
        VBox content = new VBox(18, heading, introduction, nextEvent.container(), lowerCards, commandHeading, commandOutput);
        content.setPadding(new Insets(40, 56, 32, 56));
        return new DashboardContent(content, nextEvent, commute, reminders);
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

    private HBox createCommandBar(CommandLineApp commandLineApp, StringWriter output, TextArea commandOutput,
            DashboardContent dashboard) {
        Label prompt = new Label(">");
        prompt.getStyleClass().add("command-prompt");
        TextField command = new TextField();
        command.setPromptText("Enter a Timey command, for example: plan /from \"COM3\" /to \"VivoCity\" /by 1830");
        command.setOnAction(event -> executeCommand(commandLineApp, output, commandOutput, command, dashboard));
        HBox.setHgrow(command, javafx.scene.layout.Priority.ALWAYS);
        HBox commandBar = new HBox(12, prompt, command);
        commandBar.setAlignment(Pos.CENTER_LEFT);
        commandBar.setPadding(new Insets(18, 56, 24, 56));
        commandBar.getStyleClass().add("command-bar");
        return commandBar;
    }

    private void executeCommand(CommandLineApp commandLineApp, StringWriter output, TextArea commandOutput,
            TextField command, DashboardContent dashboard) {
        String input = command.getText();
        if (input.isBlank()) {
            return;
        }
        int outputStart = output.getBuffer().length();
        CommandExecutionResult result = commandLineApp.executeCommand(input);
        commandOutput.appendText("\n> " + input + "\n" + output.getBuffer().substring(outputStart));
        refreshDashboard(dashboard, result.dashboardState());
        command.clear();
        if (result.sessionEnded()) {
            command.setDisable(true);
            command.setPromptText("This command session has ended");
        }
    }

    private void refreshDashboard(DashboardContent dashboard, DashboardState state) {
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
            dashboard.commute().title().setText(state.alternatives().isEmpty() ? "Waiting for a plan"
                    : state.alternatives().size() + " route alternatives ready");
            dashboard.commute().message().setText(state.planningMessages().isEmpty()
                    ? "Live rail alternatives are requested only after you plan a commute."
                    : String.join(" ", state.planningMessages()));
        });
        dashboard.reminders().title().setText(state.reminders().isEmpty() ? "No active reminders"
                : state.reminders().size() + " active departure reminder");
        dashboard.reminders().message().setText(state.reminders().isEmpty()
                ? "Timey will automatically schedule a departure reminder after you choose a route."
                : "Next reminder: " + state.reminders().getFirst().triggerAt().atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")));
    }

    private record Card(VBox container, Label title, Label message) {
    }

    private record DashboardContent(VBox content, Card nextEvent, Card commute, Card reminders) {
    }
}
