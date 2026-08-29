package timey.infrastructure.transit;

import java.time.Duration;
import java.util.List;

import timey.domain.transit.RouteAlternative;
import timey.ports.TransitPlanner;

/** Fixed sample route data used when live rail data is unavailable. */
public final class MockTransitPlanner implements TransitPlanner {
    private static final List<RouteAlternative> SAMPLE_ROUTES = List.of(
            new RouteAlternative("Fastest Transit", Duration.ofMinutes(8), Duration.ofMinutes(35), 1),
            new RouteAlternative("Direct Bus", Duration.ofMinutes(12), Duration.ofMinutes(47), 0));

    @Override
    public List<RouteAlternative> findRoutes(String origin, String destination) {
        if (origin == null || origin.isBlank() || destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Origin and destination must both be provided.");
        }
        return SAMPLE_ROUTES;
    }
}
