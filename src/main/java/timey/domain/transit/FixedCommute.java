package timey.domain.transit;

import java.time.Duration;

/** A user-recorded fixed duration between two locations. */
public record FixedCommute(String origin, String destination, Duration duration) {
    /** Performs this operation. */
    public FixedCommute {
        origin = normalizeLocation(origin, "Origin");
        destination = normalizeLocation(destination, "Destination");
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Fixed duration must be greater than zero.");
        }
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
