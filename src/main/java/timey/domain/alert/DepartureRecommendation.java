package timey.domain.alert;

import java.time.Duration;
import java.time.LocalTime;

/** The calculated leave-by time for a selected commute route. */
public record DepartureRecommendation(String routeName, LocalTime departureTime,
        Duration travelDuration, Duration buffer) {
    /** Performs this operation. */
    public DepartureRecommendation {
        if (routeName == null || routeName.isBlank()) {
            throw new IllegalArgumentException("Route name must not be blank.");
        }
        if (departureTime == null) {
            throw new IllegalArgumentException("Departure time must be provided.");
        }
        if (travelDuration == null || travelDuration.isNegative()) {
            throw new IllegalArgumentException("Travel duration must not be negative.");
        }
        if (buffer == null || buffer.isNegative()) {
            throw new IllegalArgumentException("Buffer must not be negative.");
        }
    }
}
