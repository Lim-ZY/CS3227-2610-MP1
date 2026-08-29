package timey.domain.alert;

import java.time.LocalDateTime;

import timey.command.PlanCommand;
import timey.domain.transit.RouteAlternative;

/** Calculates a leave-by time from an arrival target, route, and personal buffer. */
public final class DepartureCalculator {
    /** Calculates the recommendation for the selected route and target arrival datetime. */
    public DepartureRecommendation calculate(PlanCommand plan, RouteAlternative route, LocalDateTime arrivalAt) {
        if (plan == null || route == null || arrivalAt == null) {
            throw new IllegalArgumentException("Plan, route, and arrival time must all be provided.");
        }
        LocalDateTime departureAt = arrivalAt.minus(route.totalDuration()).minus(plan.getBuffer());
        return new DepartureRecommendation(
                route.name(),
                arrivalAt,
                departureAt,
                route.totalDuration(),
                plan.getBuffer());
    }
}
