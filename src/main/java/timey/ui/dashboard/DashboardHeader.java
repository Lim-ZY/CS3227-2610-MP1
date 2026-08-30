package timey.ui.dashboard;

import static java.util.Objects.requireNonNull;

import java.time.Clock;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import timey.ui.UiPart;

/** FXML-backed header containing the dashboard title and live clock. */
public final class DashboardHeader extends UiPart<HBox> {
    private static final String FXML = "DashboardHeader.fxml";

    private Timeline clockTimer;

    @FXML
    private Label clockLabel;

    /** Creates a new DashboardHeader. */
    public DashboardHeader() {
        super(FXML);
    }

    /** Starts the header clock using the supplied time source. */
    public void startClock(Clock clock) {
        stopClock();
        Clock headerClock = requireNonNull(clock);
        clockTimer = new Timeline(new KeyFrame(javafx.util.Duration.ZERO,
                event -> clockLabel.setText(DashboardClockText.now(headerClock))),
                new KeyFrame(javafx.util.Duration.seconds(1)));
        clockTimer.setCycleCount(Timeline.INDEFINITE);
        clockTimer.play();
    }

    /** Stops the header clock when its window is hidden. */
    public void stopClock() {
        if (clockTimer != null) {
            clockTimer.stop();
        }
    }

}
