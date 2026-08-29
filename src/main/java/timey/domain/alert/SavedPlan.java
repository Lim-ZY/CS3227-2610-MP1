package timey.domain.alert;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** A selected commute plan ready for local persistence. */
public record SavedPlan(LocalDate date, LocalTime arrivalTime, String origin, String destination, LocalTime leaveBy) {
    /** Performs this operation. */
    public SavedPlan {
        Objects.requireNonNull(date, "Date must be provided.");
        Objects.requireNonNull(arrivalTime, "Arrival time must be provided.");
        Objects.requireNonNull(leaveBy, "Leave-by time must be provided.");
        origin = normalizeLocation(origin, "Origin");
        destination = normalizeLocation(destination, "Destination");
    }

    private static String normalizeLocation(String location, String locationType) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException(locationType + " must not be blank.");
        }
        if (location.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(locationType + " must not contain control characters.");
        }
        return location.strip();
    }
}
