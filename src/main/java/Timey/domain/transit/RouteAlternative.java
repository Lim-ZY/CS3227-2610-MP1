package Timey.domain.transit;

import java.time.Duration;

/** A single public-transport option between an origin and destination. */
public record RouteAlternative(String name, Duration walkingDuration, Duration transitDuration, int transferCount) {
    public RouteAlternative {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Route name must not be blank.");
        }
        if (walkingDuration == null || walkingDuration.isNegative()) {
            throw new IllegalArgumentException("Walking duration must not be negative.");
        }
        if (transitDuration == null || transitDuration.isNegative()) {
            throw new IllegalArgumentException("Transit duration must not be negative.");
        }
        if (transferCount < 0) {
            throw new IllegalArgumentException("Transfer count must not be negative.");
        }
    }

    /** Returns the combined walking and in-transit duration. */
    public Duration totalDuration() {
        return walkingDuration.plus(transitDuration);
    }
}
