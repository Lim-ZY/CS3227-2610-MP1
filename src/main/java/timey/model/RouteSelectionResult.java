package timey.model;

import java.util.Objects;
import java.util.Optional;

import timey.domain.alert.DepartureRecommendation;

/** The outcome of selecting a route alternative from the current plan. */
public record RouteSelectionResult(Status status, int alternativeCount,
        Optional<DepartureRecommendation> recommendation) {
    /** Performs this operation. */
    public RouteSelectionResult {
        Objects.requireNonNull(status);
        if (alternativeCount < 0) {
            throw new IllegalArgumentException("Alternative count must not be negative.");
        }
        recommendation = Optional.ofNullable(recommendation).orElseThrow();
    }

    public static RouteSelectionResult noPlan() {
        return new RouteSelectionResult(Status.NO_PLAN, 0, Optional.empty());
    }

    public static RouteSelectionResult missingNumber(int alternativeCount) {
        return new RouteSelectionResult(Status.MISSING_NUMBER, alternativeCount, Optional.empty());
    }

    public static RouteSelectionResult invalidNumber(int alternativeCount) {
        return new RouteSelectionResult(Status.INVALID_NUMBER, alternativeCount, Optional.empty());
    }

    public static RouteSelectionResult noAlternatives() {
        return new RouteSelectionResult(Status.NO_ALTERNATIVES, 0, Optional.empty());
    }

    public static RouteSelectionResult alreadySelected() {
        return new RouteSelectionResult(Status.ALREADY_SELECTED, 0, Optional.empty());
    }

    public static RouteSelectionResult leaveNow(DepartureRecommendation recommendation) {
        return new RouteSelectionResult(Status.LEAVE_NOW, 0, Optional.of(recommendation));
    }

    /** Returns a successful route-selection result for a future departure. */
    public static RouteSelectionResult routeSelected(DepartureRecommendation recommendation) {
        return new RouteSelectionResult(Status.ROUTE_SELECTED, 0, Optional.of(recommendation));
    }

    /** Route selection states that require distinct user feedback. */
    public enum Status {
        NO_PLAN,
        MISSING_NUMBER,
        INVALID_NUMBER,
        NO_ALTERNATIVES,
        ALREADY_SELECTED,
        LEAVE_NOW,
        ROUTE_SELECTED
    }
}
