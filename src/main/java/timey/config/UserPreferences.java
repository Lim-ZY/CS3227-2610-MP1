package timey.config;

import java.time.Duration;
import java.util.List;

/** Local, durable preferences used when a plan omits optional settings. */
public record UserPreferences(Duration defaultDepartureBuffer, List<String> savedLocations) {
    /** Performs this operation. */
    public UserPreferences {
        if (defaultDepartureBuffer == null || defaultDepartureBuffer.isNegative()) {
            throw new IllegalArgumentException("Default departure buffer must not be negative.");
        }
        savedLocations = List.copyOf(savedLocations);
    }
}
