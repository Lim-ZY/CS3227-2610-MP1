package timey.domain.transit;

import java.time.Duration;

/** A human-readable walking or rail segment within a route alternative. */
public record RouteStep(RouteStepMode mode, String from, String to, String service, Duration duration) {
    /** Performs this operation. */
    public RouteStep {
        if (mode == null) {
            throw new IllegalArgumentException("Route step mode must be provided.");
        }
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new IllegalArgumentException("Route step endpoints must not be blank.");
        }
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("Route step service must not be blank.");
        }
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException("Route step duration must not be negative.");
        }
    }

    /** Formats this step for a concise command-line route breakdown. */
    public String description() {
        return mode == RouteStepMode.WALK ? "Walk from " + from + " to " + to
                : "Take " + service + " from " + from + " to " + to;
    }
}
