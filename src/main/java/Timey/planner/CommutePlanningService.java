package Timey.planner;

import java.util.List;
import java.util.Objects;

import Timey.parser.PlanCommand;
import Timey.domain.alert.DepartureCalculator;
import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.transit.RouteAlternative;
import Timey.ports.TransitPlanner;

/** Coordinates route lookup for a validated commute request. */
public final class CommutePlanningService {
    private final TransitPlanner transitPlanner;
    private final DepartureCalculator departureCalculator;

    public CommutePlanningService(TransitPlanner transitPlanner) {
        this(transitPlanner, new DepartureCalculator());
    }

    CommutePlanningService(TransitPlanner transitPlanner, DepartureCalculator departureCalculator) {
        this.transitPlanner = Objects.requireNonNull(transitPlanner);
        this.departureCalculator = Objects.requireNonNull(departureCalculator);
    }

    public List<RouteAlternative> findAlternatives(PlanCommand plan) {
        Objects.requireNonNull(plan);
        return transitPlanner.findRoutes(plan.origin(), plan.destination());
    }

    /** Calculates the leave-by recommendation for a chosen route. */
    public DepartureRecommendation recommendDeparture(PlanCommand plan, RouteAlternative route) {
        return departureCalculator.calculate(plan, route);
    }
}
