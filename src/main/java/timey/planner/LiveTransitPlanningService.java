package timey.planner;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import timey.command.PlanCommand;
import timey.domain.alert.DepartureCalculator;
import timey.domain.location.ResolvedLocation;
import timey.domain.transit.LiveRouteLookup;
import timey.ports.LiveTransitPlanner;

/** Aligns a bounded live public-transport lookup with a user's requested arrival time. */
public final class LiveTransitPlanningService {
    private final LiveTransitPlanner liveTransitPlanner;
    private final DepartureCalculator departureCalculator;
    private final Clock clock;

    public LiveTransitPlanningService(LiveTransitPlanner liveTransitPlanner, Clock clock) {
        this(liveTransitPlanner, new DepartureCalculator(), clock);
    }

    LiveTransitPlanningService(LiveTransitPlanner liveTransitPlanner, DepartureCalculator departureCalculator,
            Clock clock) {
        this.liveTransitPlanner = Objects.requireNonNull(liveTransitPlanner);
        this.departureCalculator = Objects.requireNonNull(departureCalculator);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Performs at most two lookups: a probe at the target-arrival time followed by
     * a refresh at the calculated leave-by time of the first returned route.
     */
    public LiveRouteLookup findAlignedRoutes(PlanCommand plan, ResolvedLocation origin, ResolvedLocation destination) {
        LocalDateTime targetArrival = nextTargetArrival(plan);
        LiveRouteLookup probe = liveTransitPlanner.findRoutes(origin, destination,
                targetArrival.toLocalDate(), targetArrival.toLocalTime());
        if (!probe.isAvailable() || probe.routes().isEmpty()) {
            return probe;
        }

        LocalDateTime calculatedDeparture = departureCalculator
                .calculate(plan, probe.routes().getFirst(), targetArrival)
                .departureAt();
        return liveTransitPlanner.findRoutes(origin, destination,
                calculatedDeparture.toLocalDate(), calculatedDeparture.toLocalTime());
    }

    private LocalDateTime nextTargetArrival(PlanCommand plan) {
        return LocalDateTime.of(LocalDate.now(clock), plan.getArrivalTime());
    }
}
