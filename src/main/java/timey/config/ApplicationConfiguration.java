package timey.config;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/** Provides application-wide constants and built-in preferences. */
public final class ApplicationConfiguration {
    public static final ZoneId TIME_ZONE = ZoneId.of("Asia/Singapore");
    private static final Duration DEFAULT_DEPARTURE_BUFFER = Duration.ofMinutes(10);
    private static final URI LIVE_DATABASE_URI = URI.create(
            "https://cs3227-mp1-worker.tcmpiano03.workers.dev");

    private ApplicationConfiguration() {
    }

    /** Returns the application-owned HTTPS endpoint for server-held live data. */
    public static Optional<URI> getLiveDataBaseUri() {
        return Optional.of(LIVE_DATABASE_URI);
    }

    /** Returns the built-in user preferences used by every application launch. */
    public static UserPreferences getUserPreferences() {
        return new UserPreferences(DEFAULT_DEPARTURE_BUFFER, List.of());
    }

}
