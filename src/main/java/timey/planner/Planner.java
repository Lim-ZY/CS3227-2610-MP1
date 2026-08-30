package timey.planner;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
    private static final String FALLBACK_ROUTE_NAME = "Offline estimate";
    private static final Duration FALLBACK_BUFFER = Duration.ofHours(1);
    private static final String INTERNET_CONNECTION_MESSAGE = "I'm so sorry, I need Internet connection to help you "
            + "plan your routes accurately.";
    private static final String RECONNECT_MESSAGE = "Please reconnect to the Internet for more accurate estimates.";
    private static final String ROUTE_NOT_FOUND_MESSAGE = "I'm so sorry, OneMap failed to find a suitable route.";
    private static final String FIXED_TIMING_SUGGESTION = "(Perhaps use `add` later to save this commute route "
            + "for future reference?)";

    private final CommutePlanningService commutePlanningService;
    private final LocationResolver locationResolver;
    private final LiveRailPlanningService liveRailPlanningService;
    private final Clock clock;

    /** Creates a new Planner. */
    public Planner(CommutePlanningService commutePlanningService, LocationResolver locationResolver,
            RailTransitPlanner railTransitPlanner, Clock clock) {
        this.commutePlanningService = Objects.requireNonNull(commutePlanningService);
        this.locationResolver = Objects.requireNonNull(locationResolver);
        this.clock = Objects.requireNonNull(clock);
        this.liveRailPlanningService = new LiveRailPlanningService(Objects.requireNonNull(railTransitPlanner),
                this.clock);
    }

    /** Finds live rail alternatives when available, otherwise deterministic alternatives. */
    public PlanningResult findAlternatives(PlanCommand plan) {
        LocationResolution origin = locationResolver.resolve(plan.getOrigin());
        if (!origin.isFound()) {
            return unavailableLocationResult(plan, origin);
        }

        LocationResolution destination = locationResolver.resolve(plan.getDestination());
        if (!destination.isFound()) {
            return unavailableLocationResult(plan, destination);
        }

        return findAlternativesForResolvedLocations(plan, origin.location().orElseThrow(),
                destination.location().orElseThrow(), new ArrayList<>());
    }

    private PlanningResult unavailableLocationResult(PlanCommand plan, LocationResolution unavailableLocation) {
        if (unavailableLocation.isLiveDataServiceUnreachable()) {
            return workerUnreachableResult(plan, List.of());
        }
        return deterministicResult(plan, List.of("Using offline estimate: " + unavailableLocation.reason()));
    }

    private PlanningResult findAlternativesForResolvedLocations(PlanCommand plan, ResolvedLocation origin,
            ResolvedLocation destination, List<String> messages) {
        messages.add("OneMap resolved your locations:");
        messages.add("- From: " + origin.address());
        messages.add("- To: " + destination.address());
        var liveRoutes = liveRailPlanningService.findAlignedRoutes(plan, origin, destination);
        if (liveRoutes.isAvailable() && !liveRoutes.routes().isEmpty()) {
            messages.add("Live rail routes were aligned with your target arrival time.");
            return new PlanningResult(liveRoutes.routes(), messages, false, List.of());
        }

        if (liveRoutes.isAvailable()) {
            messages.add("OneMap returned no live rail routes; using offline estimate.");
        } else {
            if (liveRoutes.isLiveDataServiceUnreachable()) {
                return workerUnreachableResult(plan, messages);
            }
            if (liveRoutes.responseStatusCode().orElse(-1) == 404) {
                messages.add(ROUTE_NOT_FOUND_MESSAGE);
                return deterministicResult(plan, messages, List.of(FIXED_TIMING_SUGGESTION));
            }
            messages.add(liveRoutes.unavailableReason().orElseThrow() + " Using offline estimate.");
        }
        return deterministicResult(plan, messages);
    }

    private PlanningResult workerUnreachableResult(PlanCommand plan, List<String> messages) {
        var unreachableMessages = new ArrayList<>(messages);
        unreachableMessages.add(INTERNET_CONNECTION_MESSAGE);
        unreachableMessages.add(RECONNECT_MESSAGE);
        return deterministicResult(plan, unreachableMessages);
    }

    private PlanningResult deterministicResult(PlanCommand plan, List<String> messages) {
        return deterministicResult(plan, messages, List.of());
    }

    private PlanningResult deterministicResult(PlanCommand plan, List<String> messages,
            List<String> routeSelectionMessages) {
        messages = new ArrayList<>(messages);
        messages.add("Using a default 1-hour buffer before your target arrival time instead of live estimates.");
        return new PlanningResult(List.of(new RouteAlternative(FALLBACK_ROUTE_NAME, Duration.ZERO, Duration.ZERO, 0)),
                messages, true, routeSelectionMessages);
    }

    /** Calculates the leave-by recommendation for a selected route alternative. */
    public DepartureRecommendation recommendDeparture(PlanCommand plan, RouteAlternative route) {
        return commutePlanningService.recommendDeparture(plan, route, nextTargetArrival(plan));
    }

    /** Calculates the fallback recommendation using the default one-hour buffer. */
    public DepartureRecommendation recommendFallbackDeparture(PlanCommand plan, RouteAlternative route) {
        LocalDateTime arrivalAt = nextTargetArrival(plan);
        return new DepartureRecommendation(route.name(), arrivalAt, arrivalAt.minus(FALLBACK_BUFFER),
                Duration.ZERO, FALLBACK_BUFFER);
    }

    private LocalDateTime nextTargetArrival(PlanCommand plan) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDateTime todayAtTarget = LocalDateTime.of(now.toLocalDate(), plan.getArrivalTime());
        return todayAtTarget.isAfter(now.toLocalDateTime()) ? todayAtTarget : todayAtTarget.plusDays(1);
    }

    /** The planned alternatives and explanation of the selected planning source. */
    public record PlanningResult(List<RouteAlternative> alternatives, List<String> messages,
            boolean usesFallbackEstimate, List<String> routeSelectionMessages) {
        /** Performs this operation. */
        public PlanningResult {
            alternatives = List.copyOf(alternatives);
            messages = List.copyOf(messages);
            routeSelectionMessages = List.copyOf(routeSelectionMessages);
        }
    }
}
