package Timey.ui.dashboard;

import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import Timey.config.ApplicationConfiguration;
import Timey.ui.DashboardState;
import Timey.ui.UiPart;

/** FXML-backed card showing scheduled departure reminders. */
public final class ReminderStatusCard extends UiPart<VBox> {
    private static final String FXML = "ReminderStatusCard.fxml";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    @FXML
    private Label title;
    @FXML
    private Label message;

    public ReminderStatusCard() {
        super(FXML);
    }

    /** Renders reminder status from the latest command-session state. */
    public void render(DashboardState state) {
        title.setText(state.reminders().isEmpty() ? "No active reminders"
                : state.reminders().size() + " active departure reminder");
        message.setText(state.reminders().isEmpty()
                ? "Timey will automatically schedule a departure reminder after you choose a route."
                : "Next reminder: " + state.reminders().getFirst().triggerAt().atZone(ApplicationConfiguration.TIME_ZONE)
                        .format(TIME_FORMAT));
    }
}
