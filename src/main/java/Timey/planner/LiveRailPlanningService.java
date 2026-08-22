package Timey.planner;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;

import Timey.parser.PlanCommand;
import Timey.domain.alert.DepartureCalculator;
import Timey.domain.location.ResolvedLocation;
import Timey.domain.transit.LiveRouteLookup;
import Timey.ports.RailTransitPlanner;

/** Aligns a bounded live rail lookup with a user's requested arrival time. */
public final class LiveRailPlanningService {
    private final RailTransitPlanner railTransitPlanner;
    private final DepartureCalculator departureCalculator;
    private final Clock clock;

    public LiveRailPlanningService(RailTransitPlanner railTransitPlanner, Clock clock) {
        this(railTransitPlanner, new DepartureCalculator(), clock);
    }

    LiveRailPlanningService(RailTransitPlanner railTransitPlanner, DepartureCalculator departureCalculator, Clock clock) {
        this.railTransitPlanner = Objects.requireNonNull(railTransitPlanner);
        this.departureCalculator = Objects.requireNonNull(departureCalculator);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Performs at most two lookups: a probe at the target-arrival time followed by
     * a refresh at the calculated leave-by time of the first returned route.
     */
    public LiveRouteLookup findAlignedRoutes(PlanCommand plan, ResolvedLocation origin, ResolvedLocation destination) {
        LocalDateTime targetArrival = nextTargetArrival(plan);
        LiveRouteLookup probe = railTransitPlanner.findRoutes(origin, destination,
                targetArrival.toLocalDate(), targetArrival.toLocalTime());
        if (!probe.isAvailable() || probe.routes().isEmpty()) {
            return probe;
        }

        LocalDateTime calculatedDeparture = targetArrival.minus(
                departureCalculator.calculate(plan, probe.routes().getFirst()).travelDuration().plus(plan.buffer()));
        return railTransitPlanner.findRoutes(origin, destination,
                calculatedDeparture.toLocalDate(), calculatedDeparture.toLocalTime());
    }

    private LocalDateTime nextTargetArrival(PlanCommand plan) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime todayAtTarget = LocalDateTime.of(now.toLocalDate(), plan.arrivalTime());
        return todayAtTarget.isAfter(now.toLocalDateTime()) ? todayAtTarget : todayAtTarget.plusDays(1);
    }
}
