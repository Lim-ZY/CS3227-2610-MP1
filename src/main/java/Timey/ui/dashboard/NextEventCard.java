package Timey.ui.dashboard;

import java.time.Clock;
import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import Timey.config.ApplicationConfiguration;
import Timey.ui.DashboardState;
import Timey.ui.UiPart;

/** FXML-backed card showing the currently planned commute and departure details. */
public final class NextEventCard extends UiPart<VBox> {
    private static final String FXML = "NextEventCard.fxml";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label title;
    @FXML
    private Label origin;
    @FXML
    private Label destination;
    @FXML
    private Label departure;
    @FXML
    private Label arrival;
    @FXML
    private Label countdown;
    @FXML
    private Label reminder;

    public NextEventCard() {
        super(FXML);
        getRoot().getStyleClass().add("next-event-card");
    }

    /** Renders the next event from the latest command-session state. */
    public void render(DashboardState state) {
        state.plan().ifPresentOrElse(plan -> {
            title.setText("Commute to " + plan.getDestination());
            origin.setText(plan.getOrigin());
            destination.setText(plan.getDestination());
            arrival.setText(TIME_FORMAT.format(plan.getArrivalTime()));
            state.recommendation().ifPresentOrElse(recommendation -> {
                departure.setText(TIME_FORMAT.format(recommendation.departureTime()));
                countdown.setText(DashboardDepartureText.until(recommendation.departureTime(),
                        Clock.system(ApplicationConfiguration.TIME_ZONE)));
                reminder.setText(state.reminders().isEmpty()
                        ? "Departure reminder will be set shortly" : "Reminder scheduled");
            }, () -> {
                departure.setText("—");
                countdown.setText("Choose a route");
                reminder.setText("Choose a route to set a reminder");
            });
        }, () -> {
            title.setText("No commute planned");
            origin.setText("—");
            destination.setText("—");
            departure.setText("—");
            arrival.setText("—");
            countdown.setText("Plan a commute");
            reminder.setText("Plan a route to set a reminder");
        });
    }
}
