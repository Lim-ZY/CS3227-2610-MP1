package timey;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

import timey.config.ApplicationConfiguration;
import timey.infrastructure.http.JdkHttpRequester;
import timey.infrastructure.http.RateLimitedHttpRequester;
import timey.infrastructure.location.OneMapLocationResolver;
import timey.infrastructure.storage.FileFixedCommuteStore;
import timey.infrastructure.storage.FilePlanStore;
import timey.infrastructure.transit.MockTransitPlanner;
import timey.infrastructure.transit.OneMapRailTransitPlanner;
import timey.parser.PlanCommandParser;
import timey.planner.CommutePlanningService;
import timey.ui.CommandLineApp;
import timey.ui.ConsoleUi;

/** Composes Timey's shared command handler for terminal and JavaFX presentations. */
public final class ApplicationFactory {
    private static final Duration ROUTING_REQUEST_TIMEOUT = Duration.ofSeconds(12);

    private ApplicationFactory() {
    }

    /** Performs this operation. */
    public static CommandLineApp createCommandLineApp(ConsoleUi ui) {
        var configuration = ApplicationConfiguration.loadDefault();
        var preferences = configuration.getUserPreferences();
        var locationResolver = new OneMapLocationResolver(new RateLimitedHttpRequester(new JdkHttpRequester()),
                configuration.getLiveDataBaseUri());
        var railTransitPlanner = new OneMapRailTransitPlanner(
                new RateLimitedHttpRequester(new JdkHttpRequester(ROUTING_REQUEST_TIMEOUT)),
                configuration.getLiveDataBaseUri());
        return new CommandLineApp(ui, new PlanCommandParser(preferences.defaultDepartureBuffer()),
                new CommutePlanningService(new MockTransitPlanner()), locationResolver, railTransitPlanner,
                Clock.system(ApplicationConfiguration.TIME_ZONE),
                new FileFixedCommuteStore(Path.of("data", "fixed-commutes.properties")),
                new FilePlanStore(Path.of("data", "plans.txt")));
    }

}
