package timey.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/** Loads optional user-specific settings without placing credentials in source control. */
public final class ApplicationConfiguration {
    private static final Path DEFAULT_PATH = Path.of("config", "application.properties");
    public static final ZoneId TIME_ZONE = ZoneId.of("Asia/Singapore");
    private static final Duration DEFAULT_DEPARTURE_BUFFER = Duration.ofMinutes(10);
    private static final URI LIVE_DATABASE_URI = URI.create(
            "https://cs3227-mp1-worker.tcmpiano03.workers.dev");

    private final Properties properties;

    private ApplicationConfiguration(Properties properties) {
        this.properties = properties;
    }

    public static ApplicationConfiguration loadDefault() {
        return load(DEFAULT_PATH);
    }

    static ApplicationConfiguration load(Path path) {
        Properties properties = new Properties();
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load " + path + ".", exception);
            }
        }
        return new ApplicationConfiguration(properties);
    }

    /** Returns the application-owned HTTPS endpoint for server-held live data. */
    public Optional<URI> getLiveDataBaseUri() {
        return Optional.of(LIVE_DATABASE_URI);
    }

    /** Loads local preferences, falling back safely when a value is absent or invalid. */
    public UserPreferences getUserPreferences() {
        return new UserPreferences(defaultDepartureBuffer(), savedLocations());
    }

    private Duration defaultDepartureBuffer() {
        String configured = properties.getProperty("departure-buffer-minutes");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_DEPARTURE_BUFFER;
        }
        try {
            long minutes = Long.parseLong(configured.trim());
            return minutes < 0 ? DEFAULT_DEPARTURE_BUFFER : Duration.ofMinutes(minutes);
        } catch (NumberFormatException | ArithmeticException exception) {
            return DEFAULT_DEPARTURE_BUFFER;
        }
    }

    private List<String> savedLocations() {
        String configured = properties.getProperty("saved-locations", "");
        return new LinkedHashSet<>(configured.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(location -> !location.isEmpty())
                .toList()).stream().toList();
    }

}
