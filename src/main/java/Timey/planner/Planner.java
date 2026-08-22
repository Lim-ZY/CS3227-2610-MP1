package Timey.planner;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import Timey.parser.PlanCommand;
import Timey.domain.location.LocationResolution;
import Timey.domain.transit.RouteAlternative;
import Timey.ports.LocationResolver;
import Timey.ports.RailTransitPlanner;

/** Coordinates live and deterministic route planning for a commute request. */
public final class Planner {
    private final CommutePlanningService commutePlanningService;
    private final LocationResolver locationResolver;
    private final LiveRailPlanningService liveRailPlanningService;

    public Planner(CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock) {
        this.commutePlanningService = Objects.requireNonNull(commutePlanningService);
        this.locationResolver = Objects.requireNonNull(locationResolver);
        this.liveRailPlanningService = new LiveRailPlanningService(Objects.requireNonNull(railTransitPlanner),
                Objects.requireNonNull(clock));
    }

    /** Finds live rail alternatives when available, otherwise deterministic alternatives. */
    public PlanningResult findAlternatives(PlanCommand plan) {
        LocationResolution origin = locationResolver.resolve(plan.origin());
        LocationResolution destination = locationResolver.resolve(plan.destination());
        List<String> messages = new ArrayList<>();
        if (origin.isFound() && destination.isFound()) {
            messages.add("OneMap resolved your locations:");
            messages.add("- From: " + origin.location().orElseThrow().address());
            messages.add("- To: " + destination.location().orElseThrow().address());
            var liveRoutes = liveRailPlanningService.findAlignedRoutes(plan,
                    origin.location().orElseThrow(), destination.location().orElseThrow());
            if (liveRoutes.isAvailable() && !liveRoutes.routes().isEmpty()) {
                messages.add("Live rail routes were aligned with your target arrival time.");
                return new PlanningResult(liveRoutes.routes(), messages);
            }
            if (liveRoutes.isAvailable()) {
                messages.add("OneMap returned no live rail routes; using deterministic routes.");
            } else {
                messages.add(liveRoutes.unavailableReason().orElseThrow() + " Using deterministic routes.");
            }
        } else {
            String reason = origin.isFound() ? destination.reason() : origin.reason();
            messages.add("Using deterministic routes: " + reason);
        }
        return new PlanningResult(commutePlanningService.findAlternatives(plan), messages);
    }

    /** The planned alternatives and explanation of the selected planning source. */
    public record PlanningResult(List<RouteAlternative> alternatives, List<String> messages) {
        public PlanningResult {
            alternatives = List.copyOf(alternatives);
            messages = List.copyOf(messages);
        }
    }
}
