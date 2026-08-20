package Timey.application;

import java.util.List;
import java.util.Objects;

import Timey.command.PlanCommand;
import Timey.domain.transit.RouteAlternative;
import Timey.ports.TransitPlanner;

/** Coordinates route lookup for a validated commute request. */
public final class CommutePlanningService {
    private final TransitPlanner transitPlanner;

    public CommutePlanningService(TransitPlanner transitPlanner) {
        this.transitPlanner = Objects.requireNonNull(transitPlanner);
    }

    public List<RouteAlternative> findAlternatives(PlanCommand plan) {
        Objects.requireNonNull(plan);
        return transitPlanner.findRoutes(plan.origin(), plan.destination());
    }
}
