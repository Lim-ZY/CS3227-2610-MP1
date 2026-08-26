package Timey;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.nio.file.Path;

import Timey.config.ApplicationConfiguration;
import Timey.config.UserPreferences;
import Timey.infrastructure.http.JdkHttpRequester;
import Timey.infrastructure.http.RetryingHttpRequester;
import Timey.infrastructure.location.OneMapLocationResolver;
import Timey.infrastructure.notification.ScheduledExecutorReminderScheduler;
import Timey.infrastructure.transit.MockTransitPlanner;
import Timey.infrastructure.transit.FileFixedCommuteStore;
import Timey.infrastructure.transit.OneMapRailTransitPlanner;
import Timey.parser.PlanCommandParser;
import Timey.planner.CommutePlanningService;
import Timey.ui.CommandLineApp;
import Timey.ui.Ui;

/** Composes Timey's shared command handler for terminal and JavaFX presentations. */
public final class ApplicationFactory {
    private static final Duration ROUTING_REQUEST_TIMEOUT = Duration.ofSeconds(12);

    private ApplicationFactory() {
    }

    public static CommandLineApp createCommandLineApp(Ui ui) {
        var configuration = ApplicationConfiguration.loadDefault();
        var preferences = configuration.userPreferences();
        var locationResolver = new OneMapLocationResolver(new RetryingHttpRequester(new JdkHttpRequester()),
                configuration.oneMapAccessToken());
        var railTransitPlanner = new OneMapRailTransitPlanner(
                new RetryingHttpRequester(new JdkHttpRequester(ROUTING_REQUEST_TIMEOUT)),
                configuration.oneMapAccessToken());
        return new CommandLineApp(ui, new PlanCommandParser(preferences.defaultDepartureBuffer()),
                new CommutePlanningService(new MockTransitPlanner()), locationResolver, railTransitPlanner,
                Clock.system(preferences.timeZone()),
                new ScheduledExecutorReminderScheduler(),
                new FileFixedCommuteStore(Path.of("data", "fixed-commutes.properties")));
    }

    /** Loads the local preferences displayed by the JavaFX dashboard. */
    public static UserPreferences loadUserPreferences() {
        return ApplicationConfiguration.loadDefault().userPreferences();
    }
}
