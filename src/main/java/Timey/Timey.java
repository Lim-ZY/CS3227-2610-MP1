package Timey;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import Timey.presentation.CommandLineApp;
import Timey.presentation.Ui;
import Timey.application.CommutePlanningService;
import Timey.command.PlanCommandParser;
import Timey.config.ApplicationConfiguration;
import Timey.infrastructure.http.JdkHttpRequester;
import Timey.infrastructure.http.RetryingHttpRequester;
import Timey.infrastructure.location.OneMapLocationResolver;
import Timey.infrastructure.notification.ScheduledExecutorReminderScheduler;
import Timey.infrastructure.transit.MockTransitPlanner;
import Timey.infrastructure.transit.OneMapRailTransitPlanner;

/** Entry point for the Timey application. */
public final class Timey {
    private static final Duration ROUTING_REQUEST_TIMEOUT = Duration.ofSeconds(12);

    private Timey() {
    }

    public static void main(String[] args) {
        var ui = new Ui();
        var configuration = ApplicationConfiguration.loadDefault();
        var locationResolver = new OneMapLocationResolver(new RetryingHttpRequester(new JdkHttpRequester()),
                configuration.oneMapAccessToken());
        var railTransitPlanner = new OneMapRailTransitPlanner(
                new RetryingHttpRequester(new JdkHttpRequester(ROUTING_REQUEST_TIMEOUT)),
                configuration.oneMapAccessToken());
        var planner = new CommutePlanningService(new MockTransitPlanner());
        new CommandLineApp(ui, new PlanCommandParser(), planner, locationResolver,
                railTransitPlanner, Clock.system(ZoneId.of("Asia/Singapore")), new ScheduledExecutorReminderScheduler()).run();
    }
}
