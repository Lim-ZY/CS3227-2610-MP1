package timey.planner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import timey.command.PlanCommand;
import timey.domain.alert.DepartureCalculator;
import timey.domain.alert.DepartureRecommendation;
import timey.domain.transit.RouteAlternative;
import timey.ports.TransitPlanner;

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

    /** Performs this operation. */
    public List<RouteAlternative> findAlternatives(PlanCommand plan) {
        Objects.requireNonNull(plan);
        return transitPlanner.findRoutes(plan.getOrigin(), plan.getDestination());
    }

    /** Calculates the leave-by recommendation for a chosen route and target arrival datetime. */
    public DepartureRecommendation recommendDeparture(PlanCommand plan, RouteAlternative route,
            LocalDateTime arrivalAt) {
        return departureCalculator.calculate(plan, route, arrivalAt);
    }
}
