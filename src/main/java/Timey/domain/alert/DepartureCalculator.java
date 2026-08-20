package Timey.domain.alert;

import Timey.command.PlanCommand;
import Timey.domain.transit.RouteAlternative;

/** Calculates a leave-by time from an arrival target, route, and personal buffer. */
public final class DepartureCalculator {
    /** Calculates the recommendation for the selected route. */
    public DepartureRecommendation calculate(PlanCommand plan, RouteAlternative route) {
        if (plan == null || route == null) {
            throw new IllegalArgumentException("Plan and route must both be provided.");
        }
        return new DepartureRecommendation(
                route.name(),
                plan.arrivalTime().minus(route.totalDuration()).minus(plan.buffer()),
                route.totalDuration(),
                plan.buffer());
    }
}
