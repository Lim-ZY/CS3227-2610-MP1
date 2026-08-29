package timey.ui.dashboard;

import static java.util.Objects.requireNonNull;

import java.time.Clock;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import timey.config.ApplicationConfiguration;
import timey.config.UserPreferences;
import timey.ui.DashboardState;
import timey.ui.UiPart;

/** FXML-backed header containing dashboard menus, summaries, and the live clock. */
public final class DashboardHeader extends UiPart<HBox> {
    private static final String FXML = "DashboardHeader.fxml";

    private final UserPreferences preferences;
    private Timeline clockTimer;
    private MenuItem recentLocations;
    private MenuItem personalBuffer;

    @FXML
    private MenuButton timeyMenu;
    @FXML
    private Label clockLabel;

    /** Creates a new DashboardHeader. */
    public DashboardHeader(UserPreferences preferences) {
        super(FXML);
        this.preferences = requireNonNull(preferences);
        configureMenu();
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

    /** Updates menu summaries from the latest command-session state. */
    public void refresh(DashboardState state) {
        recentLocations.setText(DashboardMenuSummary.recentLocations(state));
        personalBuffer.setText(DashboardMenuSummary.personalBuffer(state, preferences));
    }

    private void configureMenu() {
        timeyMenu.getItems().addAll(savedLocationsMenu(), recentLocationsMenu(), preferencesMenu());
    }

    private Menu savedLocationsMenu() {
        Menu savedLocations = new Menu("Saved locations");
        if (preferences.savedLocations().isEmpty()) {
            savedLocations.getItems().add(new MenuItem("No saved locations yet"));
        } else {
            preferences.savedLocations().forEach(location -> savedLocations.getItems().add(new MenuItem(location)));
        }
        return savedLocations;
    }

    private Menu recentLocationsMenu() {
        recentLocations = new MenuItem("No recent plan");
        Menu recent = new Menu("Recent locations");
        recent.getItems().add(recentLocations);
        return recent;
    }

    private Menu preferencesMenu() {
        personalBuffer = new MenuItem("Set per plan");
        MenuItem timeZone = new MenuItem(ApplicationConfiguration.TIME_ZONE.getId());
        Menu preferenceMenu = new Menu("Preferences");
        preferenceMenu.getItems().addAll(new Menu("Personal buffer", null, personalBuffer),
                new Menu("Time zone", null, timeZone));
        return preferenceMenu;
    }
}
