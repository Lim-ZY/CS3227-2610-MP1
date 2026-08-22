package Timey.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/** Loads optional user-specific settings without placing credentials in source control. */
public final class ApplicationConfiguration {
    private static final Path DEFAULT_PATH = Path.of("config", "application.properties");
    private static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("Asia/Singapore");
    private static final Duration DEFAULT_DEPARTURE_BUFFER = Duration.ofMinutes(10);

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

    public Optional<String> oneMapAccessToken() {
        return firstNonBlank(System.getenv("ONEMAP_ACCESS_TOKEN"), properties.getProperty("onemap.access-token"));
    }

    /** Loads local preferences, falling back safely when a value is absent or invalid. */
    public UserPreferences userPreferences() {
        return new UserPreferences(timeZone(), defaultDepartureBuffer(), savedLocations());
    }

    private ZoneId timeZone() {
        String configured = properties.getProperty("timezone");
        if (configured == null || configured.isBlank()) {
            return DEFAULT_TIME_ZONE;
        }
        try {
            return ZoneId.of(configured.trim());
        } catch (DateTimeException exception) {
            return DEFAULT_TIME_ZONE;
        }
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

    private Optional<String> firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return Optional.of(first.trim());
        }
        if (second != null && !second.isBlank()) {
            return Optional.of(second.trim());
        }
        return Optional.empty();
    }
}
