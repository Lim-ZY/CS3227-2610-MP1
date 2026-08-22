package Timey.ui.dashboard;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** JavaFX presentation shell for Timey's dashboard. */
public final class TimeyDashboardApp extends Application {
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dashboard");
        root.setTop(createHeader());
        root.setCenter(createEmptyDashboard());
        root.setBottom(createCommandBar());

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

    private VBox createEmptyDashboard() {
        Label heading = new Label("Your day, on track.");
        heading.getStyleClass().add("page-heading");
        Label introduction = new Label("Plan a commute in the command bar to see your next event and departure plan here.");
        introduction.getStyleClass().add("muted");

        VBox nextEvent = card("NEXT EVENT", "No commute planned", "Your route, departure time, and reminder will appear here.");
        VBox commute = card("COMMUTE STATUS", "Waiting for a plan", "Live rail alternatives are requested only after you plan a commute.");
        VBox reminders = card("REMINDER STATUS", "No active reminders", "Timey will automatically schedule a departure reminder after you choose a route.");
        HBox lowerCards = new HBox(18, commute, reminders);
        lowerCards.getChildren().forEach(card -> HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS));

        VBox content = new VBox(18, heading, introduction, nextEvent, lowerCards);
        content.setPadding(new Insets(40, 56, 32, 56));
        return content;
    }

    private VBox card(String label, String title, String message) {
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
        return card;
    }

    private HBox createCommandBar() {
        Label prompt = new Label(">");
        prompt.getStyleClass().add("command-prompt");
        TextField command = new TextField();
        command.setPromptText("Embedded command panel will be connected in the next iteration");
        command.setDisable(true);
        HBox.setHgrow(command, javafx.scene.layout.Priority.ALWAYS);
        HBox commandBar = new HBox(12, prompt, command);
        commandBar.setAlignment(Pos.CENTER_LEFT);
        commandBar.setPadding(new Insets(18, 56, 24, 56));
        commandBar.getStyleClass().add("command-bar");
        return commandBar;
    }
}
