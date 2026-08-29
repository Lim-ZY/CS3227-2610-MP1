package timey.planner;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import timey.command.PlanCommand;
import timey.domain.alert.DepartureRecommendation;
import timey.domain.location.LocationResolution;
import timey.domain.location.ResolvedLocation;
import timey.domain.transit.RouteAlternative;
import timey.ports.LocationResolver;
import timey.ports.RailTransitPlanner;

/** Coordinates live and deterministic route planning for a commute request. */
public final class Planner {
    private final CommutePlanningService commutePlanningService;
    private final LocationResolver locationResolver;
    private final LiveRailPlanningService liveRailPlanningService;

    /** Creates a new Planner. */
    public Planner(CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock) {
        this.commutePlanningService = Objects.requireNonNull(commutePlanningService);
        this.locationResolver = Objects.requireNonNull(locationResolver);
        this.liveRailPlanningService = new LiveRailPlanningService(Objects.requireNonNull(railTransitPlanner),
                Objects.requireNonNull(clock));
    }

    /** Finds live rail alternatives when available, otherwise deterministic alternatives. */
    public PlanningResult findAlternatives(PlanCommand plan) {
        LocationResolution origin = locationResolver.resolve(plan.getOrigin());
        LocationResolution destination = locationResolver.resolve(plan.getDestination());
        List<String> messages = new ArrayList<>();
        if (!origin.isFound() || !destination.isFound()) {
            String reason = origin.isFound() ? destination.reason() : origin.reason();
            messages.add("Using deterministic routes: " + reason);
            return deterministicResult(plan, messages);
        }

        return findAlternativesForResolvedLocations(plan, origin.location().orElseThrow(),
                destination.location().orElseThrow(), messages);
    }

    private PlanningResult findAlternativesForResolvedLocations(PlanCommand plan, ResolvedLocation origin,
            ResolvedLocation destination, List<String> messages) {
        messages.add("OneMap resolved your locations:");
        messages.add("- From: " + origin.address());
        messages.add("- To: " + destination.address());
        var liveRoutes = liveRailPlanningService.findAlignedRoutes(plan, origin, destination);
        if (liveRoutes.isAvailable() && !liveRoutes.routes().isEmpty()) {
            messages.add("Live rail routes were aligned with your target arrival time.");
            return new PlanningResult(liveRoutes.routes(), messages);
        }

        if (liveRoutes.isAvailable()) {
            messages.add("OneMap returned no live rail routes; using deterministic routes.");
        } else {
            messages.add(liveRoutes.unavailableReason().orElseThrow() + " Using deterministic routes.");
        }
        return deterministicResult(plan, messages);
    }

    private PlanningResult deterministicResult(PlanCommand plan, List<String> messages) {
        return new PlanningResult(commutePlanningService.findAlternatives(plan), messages);
    }

    /** Calculates the leave-by recommendation for a selected route alternative. */
    public DepartureRecommendation recommendDeparture(PlanCommand plan, RouteAlternative route) {
        return commutePlanningService.recommendDeparture(plan, route);
    }

    /** The planned alternatives and explanation of the selected planning source. */
    public record PlanningResult(List<RouteAlternative> alternatives, List<String> messages) {
        /** Performs this operation. */
        public PlanningResult {
            alternatives = List.copyOf(alternatives);
            messages = List.copyOf(messages);
        }
    }
}
