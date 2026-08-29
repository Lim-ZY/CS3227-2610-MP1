package timey.infrastructure.transit;

import java.util.List;

import timey.domain.transit.RouteAlternative;
import timey.ports.TransitPlanner;

/** Transit planner test double used when no live planner is required. */
public final class MockTransitPlanner implements TransitPlanner {
    @Override
    public List<RouteAlternative> findRoutes(String origin, String destination) {
        if (origin == null || origin.isBlank() || destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Origin and destination must both be provided.");
        }
        return List.of();
    }
}
