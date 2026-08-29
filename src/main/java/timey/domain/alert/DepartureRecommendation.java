package timey.domain.alert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** The calculated leave-by time for a selected commute route. */
public record DepartureRecommendation(String routeName, LocalDateTime arrivalAt, LocalDateTime departureAt,
        Duration travelDuration, Duration buffer) {
    /** Performs this operation. */
    public DepartureRecommendation {
        if (routeName == null || routeName.isBlank()) {
            throw new IllegalArgumentException("Route name must not be blank.");
        }
        if (arrivalAt == null || departureAt == null) {
            throw new IllegalArgumentException("Arrival and departure times must be provided.");
        }
        if (travelDuration == null || travelDuration.isNegative()) {
            throw new IllegalArgumentException("Travel duration must not be negative.");
        }
        if (buffer == null || buffer.isNegative()) {
            throw new IllegalArgumentException("Buffer must not be negative.");
        }
        if (!departureAt.equals(arrivalAt.minus(travelDuration).minus(buffer))) {
            throw new IllegalArgumentException(
                    "Departure time must match the arrival time, travel duration, and buffer.");
        }
    }

    /** Returns the local leave-by time for presentation. */
    public LocalTime departureTime() {
        return departureAt.toLocalTime();
    }
}
