package Timey;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import Timey.config.ApplicationConfiguration;
import Timey.infrastructure.http.JdkHttpRequester;
import Timey.infrastructure.http.RetryingHttpRequester;
import Timey.infrastructure.location.OneMapLocationResolver;
import Timey.infrastructure.notification.ScheduledExecutorReminderScheduler;
import Timey.infrastructure.transit.MockTransitPlanner;
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
        var locationResolver = new OneMapLocationResolver(new RetryingHttpRequester(new JdkHttpRequester()),
                configuration.oneMapAccessToken());
        var railTransitPlanner = new OneMapRailTransitPlanner(
                new RetryingHttpRequester(new JdkHttpRequester(ROUTING_REQUEST_TIMEOUT)),
                configuration.oneMapAccessToken());
        return new CommandLineApp(ui, new PlanCommandParser(), new CommutePlanningService(new MockTransitPlanner()),
                locationResolver, railTransitPlanner, Clock.system(ZoneId.of("Asia/Singapore")),
                new ScheduledExecutorReminderScheduler());
    }
}
