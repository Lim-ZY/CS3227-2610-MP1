package timey.ui.dashboard;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import timey.config.ApplicationConfiguration;
import timey.domain.alert.SavedPlan;
import timey.ui.DashboardState;
import timey.ui.UiPart;

/** FXML-backed card showing the currently planned commute and departure details. */
public final class NextEventCard extends UiPart<VBox> {
    private static final String FXML = "NextEventCard.fxml";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock clock;

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
    private Label cardLabel;

    /** Creates a new NextEventCard. */
    public NextEventCard() {
        this(Clock.system(ApplicationConfiguration.TIME_ZONE));
    }

    NextEventCard(Clock clock) {
        super(FXML);
        this.clock = Objects.requireNonNull(clock);
        getRoot().getStyleClass().add("next-event-card");
    }

    /** Renders the next event from the latest command-session state. */
    public void render(DashboardState state) {
        cardLabel.setText(state.plan().isPresent() && state.recommendation().isEmpty()
                ? "PLANNING EVENT..." : "UPCOMING EVENT");
        nextEvent(state)
                .ifPresentOrElse(event -> {
                    title.setText("Commute to " + event.destination());
                    origin.setText(event.origin());
                    destination.setText(event.destination());
                    arrival.setText(TIME_FORMAT.format(event.arrivalTime()));
                    event.departureTime().ifPresentOrElse(departureTime -> {
                        departure.setText(TIME_FORMAT.format(departureTime));
                        countdown.setText(DashboardDepartureText.until(event.departureAt().orElseThrow(), clock));
                    }, () -> {
                        departure.setText("—");
                        countdown.setText("Choose a route");
                    });
                }, () -> {
                    title.setText("No commute planned");
                    origin.setText("—");
                    destination.setText("—");
                    departure.setText("—");
                    arrival.setText("—");
                    countdown.setText("Plan a commute");
                });
    }

    private Optional<EventDetails> nextEvent(DashboardState state) {
        Optional<EventDetails> plannedEvent = state.plan().map(plan -> new EventDetails(plan.getOrigin(),
                plan.getDestination(), plan.getArrivalTime(),
                state.recommendation().map(recommendation -> recommendation.departureTime()),
                state.recommendation().map(recommendation -> recommendation.departureAt())));
        Optional<EventDetails> savedEvent = state.nextSavedPlan().map(this::eventDetails);

        if (plannedEvent.isEmpty() || state.recommendation().isEmpty()) {
            return plannedEvent.or(() -> savedEvent);
        }
        EventDetails planned = plannedEvent.orElseThrow();
        return savedEvent.map(saved -> planned.departureAt().orElseThrow().isBefore(saved.departureAt()
                .orElseThrow()) ? planned : saved);
    }

    private EventDetails eventDetails(SavedPlan plan) {
        LocalDateTime departureAt = LocalDateTime.of(
                plan.leaveBy().isAfter(plan.arrivalTime()) ? plan.date().minusDays(1) : plan.date(), plan.leaveBy());
        return new EventDetails(plan.origin(), plan.destination(), plan.arrivalTime(), Optional.of(plan.leaveBy()),
                Optional.of(departureAt));
    }

    private record EventDetails(String origin, String destination, LocalTime arrivalTime,
            Optional<LocalTime> departureTime, Optional<LocalDateTime> departureAt) {
    }

}
