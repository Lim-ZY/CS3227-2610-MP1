package Timey;

import java.time.Clock;
import java.time.Duration;
import java.nio.file.Path;

import Timey.config.ApplicationConfiguration;
import Timey.config.UserPreferences;
import Timey.infrastructure.http.JdkHttpRequester;
import Timey.infrastructure.http.RetryingHttpRequester;
import Timey.infrastructure.alert.FilePlanStore;
import Timey.infrastructure.location.OneMapLocationResolver;
import Timey.infrastructure.notification.ScheduledExecutorReminderScheduler;
import Timey.infrastructure.transit.MockTransitPlanner;
import Timey.infrastructure.transit.FileFixedCommuteStore;
import Timey.infrastructure.transit.OneMapRailTransitPlanner;
import Timey.parser.PlanCommandParser;
import Timey.planner.CommutePlanningService;
import Timey.ui.CommandLineApp;
import Timey.ui.ConsoleUi;

/** Composes Timey's shared command handler for terminal and JavaFX presentations. */
public final class ApplicationFactory {
    private static final Duration ROUTING_REQUEST_TIMEOUT = Duration.ofSeconds(12);

    private ApplicationFactory() {
    }

    public static CommandLineApp createCommandLineApp(ConsoleUi ui) {
        var configuration = ApplicationConfiguration.loadDefault();
        var preferences = configuration.getUserPreferences();
        var locationResolver = new OneMapLocationResolver(new RetryingHttpRequester(new JdkHttpRequester()),
                configuration.getLiveDataBaseUri());
        var railTransitPlanner = new OneMapRailTransitPlanner(
                new RetryingHttpRequester(new JdkHttpRequester(ROUTING_REQUEST_TIMEOUT)),
                configuration.getLiveDataBaseUri());
        return new CommandLineApp(ui, new PlanCommandParser(preferences.defaultDepartureBuffer()),
                new CommutePlanningService(new MockTransitPlanner()), locationResolver, railTransitPlanner,
                Clock.system(ApplicationConfiguration.TIME_ZONE),
                new ScheduledExecutorReminderScheduler(),
                new FileFixedCommuteStore(Path.of("data", "fixed-commutes.properties")),
                new FilePlanStore(Path.of("data", "plans.txt")));
    }

    /** Loads the local preferences displayed by the JavaFX dashboard. */
    public static UserPreferences loadUserPreferences() {
        return ApplicationConfiguration.loadDefault().getUserPreferences();
    }
}
