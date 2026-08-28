package Timey.ui.dashboard;

import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import Timey.ui.DashboardState;
import Timey.ui.UiPart;

/** FXML-backed card showing the current commute-planning status. */
public final class CommuteStatusCard extends UiPart<VBox> {
    private static final String FXML = "CommuteStatusCard.fxml";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label title;
    @FXML
    private Label message;

    public CommuteStatusCard() {
        super(FXML);
    }

    /** Renders commute status from the latest command-session state. */
    public void render(DashboardState state) {
        state.recommendation().ifPresentOrElse(recommendation -> {
            title.setText("Leave by " + TIME_FORMAT.format(recommendation.departureTime()));
            message.setText(recommendation.routeName() + " · " + recommendation.travelDuration().toMinutes()
                    + " minute commute");
        }, () -> {
            DashboardCommuteStatus status = DashboardCommuteStatus.from(state);
            title.setText(status.title());
            message.setText(status.message());
        });
    }

    /** Displays the current command's loading state. */
    public void showLoading() {
        title.setText("Updating commute…");
        message.setText("Looking up locations and live rail alternatives. Please wait.");
    }

    /** Displays a command execution failure without altering the prior plan. */
    public void showFailure(Throwable failure) {
        title.setText("Could not update commute");
        message.setText("Your previous plan is unchanged. "
                + (failure.getMessage() == null ? "Please try again." : failure.getMessage()));
    }
}
