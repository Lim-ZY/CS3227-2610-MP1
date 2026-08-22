package Timey.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/** Local, durable preferences used when a plan omits optional settings. */
public record UserPreferences(ZoneId timeZone, Duration defaultDepartureBuffer, List<String> savedLocations) {
    public UserPreferences {
        if (timeZone == null) {
            throw new IllegalArgumentException("Time zone must be provided.");
        }
        if (defaultDepartureBuffer == null || defaultDepartureBuffer.isNegative()) {
            throw new IllegalArgumentException("Default departure buffer must not be negative.");
        }
        savedLocations = List.copyOf(savedLocations);
    }
}
