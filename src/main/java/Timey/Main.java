package Timey;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import Timey.presentation.CommandLineApp;
import Timey.application.CommutePlanningService;
import Timey.command.PlanCommandParser;
import Timey.config.ApplicationConfiguration;
import Timey.infrastructure.http.JdkHttpRequester;
import Timey.infrastructure.location.OneMapLocationResolver;
import Timey.infrastructure.transit.MockTransitPlanner;
import Timey.infrastructure.transit.OneMapRailTransitPlanner;

/** Entry point for the Timey application. */
public final class Main {
    private static final Duration ROUTING_REQUEST_TIMEOUT = Duration.ofSeconds(12);

    private Main() {
    }

    public static void main(String[] args) {
        var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        var output = new PrintWriter(System.out, true);
        var configuration = ApplicationConfiguration.loadDefault();
        var locationResolver = new OneMapLocationResolver(new JdkHttpRequester(), configuration.oneMapAccessToken());
        var railTransitPlanner = new OneMapRailTransitPlanner(new JdkHttpRequester(ROUTING_REQUEST_TIMEOUT),
                configuration.oneMapAccessToken());
        var planner = new CommutePlanningService(new MockTransitPlanner());
        new CommandLineApp(input, output, new PlanCommandParser(), planner, locationResolver,
                railTransitPlanner, Clock.system(ZoneId.of("Asia/Singapore"))).run();
    }
}
