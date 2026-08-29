package timey.model;

import java.util.Objects;
import java.util.Optional;

import timey.domain.alert.DepartureRecommendation;
import timey.domain.alert.ScheduledDepartureReminder;

/** The outcome of selecting a route alternative from the current plan. */
public record RouteSelectionResult(Status status, int alternativeCount,
        Optional<DepartureRecommendation> recommendation, Optional<ScheduledDepartureReminder> reminder) {
    /** Performs this operation. */
    public RouteSelectionResult {
        Objects.requireNonNull(status);
        if (alternativeCount < 0) {
            throw new IllegalArgumentException("Alternative count must not be negative.");
        }
        recommendation = Optional.ofNullable(recommendation).orElseThrow();
        reminder = Optional.ofNullable(reminder).orElseThrow();
    }

    public static RouteSelectionResult noPlan() {
        return new RouteSelectionResult(Status.NO_PLAN, 0, Optional.empty(), Optional.empty());
    }

    public static RouteSelectionResult missingNumber(int alternativeCount) {
        return new RouteSelectionResult(Status.MISSING_NUMBER, alternativeCount, Optional.empty(), Optional.empty());
    }

    public static RouteSelectionResult invalidNumber(int alternativeCount) {
        return new RouteSelectionResult(Status.INVALID_NUMBER, alternativeCount, Optional.empty(), Optional.empty());
    }

    public static RouteSelectionResult leaveNow(DepartureRecommendation recommendation) {
        return new RouteSelectionResult(Status.LEAVE_NOW, 0, Optional.of(recommendation), Optional.empty());
    }

    /** Performs this operation. */
    public static RouteSelectionResult reminderScheduled(DepartureRecommendation recommendation,
            ScheduledDepartureReminder reminder) {
        return new RouteSelectionResult(Status.REMINDER_SCHEDULED, 0, Optional.of(recommendation),
                Optional.of(reminder));
    }

    /** Route selection states that require distinct user feedback. */
    public enum Status {
        NO_PLAN,
        MISSING_NUMBER,
        INVALID_NUMBER,
        LEAVE_NOW,
        REMINDER_SCHEDULED
    }
}
