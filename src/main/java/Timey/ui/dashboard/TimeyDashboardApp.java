package Timey.ui.dashboard;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Clock;

import Timey.ApplicationFactory;
import Timey.config.UserPreferences;
import Timey.config.ApplicationConfiguration;
import Timey.ui.CommandLineApp;
import Timey.ui.CommandExecutionResult;
import Timey.ui.DashboardState;
import Timey.ui.ConsoleUi;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import java.time.format.DateTimeFormatter;

/** JavaFX presentation shell for Timey's dashboard. */
public final class TimeyDashboardApp extends Application {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void start(Stage stage) {
        UserPreferences preferences = ApplicationFactory.loadUserPreferences();
        Header header = createHeader(preferences);
        StringWriter output = new StringWriter();
        CommandLineApp commandLineApp = ApplicationFactory.createCommandLineApp(
                new ConsoleUi(new BufferedReader(new StringReader("")), new PrintWriter(output, true)));
        TextArea commandOutput = createCommandOutput();
        DashboardContent dashboard = createDashboard(commandOutput);
        MainWindow mainWindow = new MainWindow(stage);
        mainWindow.setHeader(header.container());
        mainWindow.setDashboardContent(dashboard.content());
        mainWindow.setCommandBar(createCommandBar(commandLineApp, output, commandOutput, dashboard, header));
        mainWindow.show();
        Timeline clockTimer = startClock(header.clock(), ApplicationConfiguration.TIME_ZONE);
        mainWindow.setOnHidden(event -> clockTimer.stop());
    }

    private Header createHeader(UserPreferences preferences) {
        MenuButton timey = new MenuButton("Timey");
        timey.getStyleClass().add("brand");
        Menu savedLocations = new Menu("Saved locations");
        if (preferences.savedLocations().isEmpty()) {
            savedLocations.getItems().add(new MenuItem("No saved locations yet"));
        } else {
            preferences.savedLocations().forEach(location -> savedLocations.getItems().add(new MenuItem(location)));
        }
        MenuItem recentLocations = new MenuItem("No recent plan");
        Menu recent = new Menu("Recent locations");
        recent.getItems().add(recentLocations);
        MenuItem personalBuffer = new MenuItem("Set per plan");
        MenuItem timeZone = new MenuItem(ApplicationConfiguration.TIME_ZONE.getId());
        Menu preferenceMenu = new Menu("Preferences");
        preferenceMenu.getItems().addAll(new Menu("Personal buffer", null, personalBuffer),
                new Menu("Time zone", null, timeZone));
        timey.getItems().addAll(savedLocations, recent, preferenceMenu);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label clock = new Label();
        clock.getStyleClass().add("dashboard-clock");
        HBox header = new HBox(16, timey, spacer, clock);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return new Header(header, recentLocations, personalBuffer, preferences, clock);
    }

    private Timeline startClock(Label clockLabel, java.time.ZoneId timeZone) {
        Clock clock = Clock.system(timeZone);
        Timeline timeline = new Timeline(new KeyFrame(javafx.util.Duration.ZERO,
                event -> clockLabel.setText(DashboardClockText.now(clock))),
                new KeyFrame(javafx.util.Duration.seconds(1)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        return timeline;
    }

    private DashboardContent createDashboard(TextArea commandOutput) {
        Label heading = new Label("Your day, on track.");
        heading.getStyleClass().add("page-heading");
        Label introduction = new Label("Plan a commute in the command bar to see your next event and departure plan here.");
        introduction.getStyleClass().add("muted");

        NextEventCard nextEvent = createNextEventCard();
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

    private NextEventCard createNextEventCard() {
        Label eventType = new Label("Physical");
        eventType.getStyleClass().add("event-type");
        Label title = new Label("No commute planned");
        title.getStyleClass().add("next-event-title");
        Label origin = locationLabel("—");
        Label destination = locationLabel("—");
        Region journeyLine = new Region();
        journeyLine.getStyleClass().add("journey-line");
        HBox.setHgrow(journeyLine, javafx.scene.layout.Priority.ALWAYS);
        HBox journey = new HBox(10, locationPin(origin), journeyLine, locationPin(destination));
        journey.setAlignment(Pos.CENTER_LEFT);
        VBox journeyDetails = new VBox(10, title, journey);
        HBox.setHgrow(journeyDetails, javafx.scene.layout.Priority.ALWAYS);

        Label countdown = new Label("Plan a commute");
        countdown.getStyleClass().add("departure-countdown");
        Label countdownCaption = new Label("time until\ndeparture");
        countdownCaption.getStyleClass().add("departure-caption");
        VBox countdownDetails = new VBox(4, countdown, countdownCaption);
        countdownDetails.setAlignment(Pos.CENTER_RIGHT);
        Label departure = new Label("—");
        Label arrival = new Label("—");
        Region timeLine = new Region();
        timeLine.getStyleClass().add("event-time-line");
        VBox schedule = new VBox(2, timePoint(departure), timeLine, timePoint(arrival));
        HBox timing = new HBox(12, countdownDetails, schedule);
        timing.setAlignment(Pos.CENTER_RIGHT);
        HBox body = new HBox(28, journeyDetails, timing);
        body.setAlignment(Pos.CENTER_LEFT);

        Label reminder = new Label("Plan a route to set a reminder");
        reminder.getStyleClass().add("reminder-text");
        Label reminderDot = new Label("●");
        reminderDot.getStyleClass().add("reminder-dot");
        HBox reminderStatus = new HBox(8, reminderDot, reminder);
        reminderStatus.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(12, eventType, body, reminderStatus);
        card.getStyleClass().addAll("card", "next-event-card");
        return new NextEventCard(card, title, origin, destination, departure, arrival, countdown, reminder);
    }

    private Label locationLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("location-label");
        return label;
    }

    private VBox locationPin(Label location) {
        Label pin = new Label("⌖");
        pin.getStyleClass().add("location-pin");
        VBox endpoint = new VBox(-2, pin, location);
        endpoint.setAlignment(Pos.CENTER);
        return endpoint;
    }

    private HBox timePoint(Label time) {
        Label dot = new Label("●");
        dot.getStyleClass().add("time-dot");
        time.getStyleClass().add("event-time");
        HBox point = new HBox(6, dot, time);
        point.setAlignment(Pos.CENTER_LEFT);
        return point;
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
            dashboard.nextEvent().title().setText("Commute to " + plan.getDestination());
            dashboard.nextEvent().origin().setText(plan.getOrigin());
            dashboard.nextEvent().destination().setText(plan.getDestination());
            dashboard.nextEvent().arrival().setText(TIME_FORMAT.format(plan.getArrivalTime()));
            state.recommendation().ifPresentOrElse(recommendation -> {
                dashboard.nextEvent().departure().setText(TIME_FORMAT.format(recommendation.departureTime()));
                dashboard.nextEvent().countdown().setText(DashboardDepartureText.until(recommendation.departureTime(),
                        Clock.system(ApplicationConfiguration.TIME_ZONE)));
                dashboard.nextEvent().reminder().setText(state.reminders().isEmpty()
                        ? "Departure reminder will be set shortly" : "Reminder scheduled");
            }, () -> {
                dashboard.nextEvent().departure().setText("—");
                dashboard.nextEvent().countdown().setText("Choose a route");
                dashboard.nextEvent().reminder().setText("Choose a route to set a reminder");
            });
        }, () -> {
            dashboard.nextEvent().title().setText("No commute planned");
            dashboard.nextEvent().origin().setText("—");
            dashboard.nextEvent().destination().setText("—");
            dashboard.nextEvent().departure().setText("—");
            dashboard.nextEvent().arrival().setText("—");
            dashboard.nextEvent().countdown().setText("Plan a commute");
            dashboard.nextEvent().reminder().setText("Plan a route to set a reminder");
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
                : "Next reminder: " + state.reminders().getFirst().triggerAt().atZone(ApplicationConfiguration.TIME_ZONE)
                        .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")));
        refreshAlternatives(dashboard.alternatives(), state);
        header.recentLocations().setText(DashboardMenuSummary.recentLocations(state));
        header.personalBuffer().setText(DashboardMenuSummary.personalBuffer(state, header.preferences()));
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

    private record DashboardContent(VBox content, NextEventCard nextEvent, Card commute, Card reminders,
            VBox alternatives) {
    }

    private record NextEventCard(VBox container, Label title, Label origin, Label destination, Label departure,
            Label arrival, Label countdown, Label reminder) {
    }

    private record Header(HBox container, MenuItem recentLocations, MenuItem personalBuffer, UserPreferences preferences,
            Label clock) {
    }

    private record DashboardCommandResponse(CommandExecutionResult result, String output) {
    }
}
