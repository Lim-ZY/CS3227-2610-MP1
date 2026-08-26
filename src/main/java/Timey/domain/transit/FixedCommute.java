package Timey.domain.transit;

import java.time.Duration;

/** A user-recorded fixed duration between two locations. */
public record FixedCommute(String origin, String destination, Duration duration) {
    public FixedCommute {
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin must not be blank.");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Destination must not be blank.");
        }
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Fixed duration must be greater than zero.");
        }
    }
}
