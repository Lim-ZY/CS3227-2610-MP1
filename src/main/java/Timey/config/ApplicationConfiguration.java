package Timey.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/** Loads optional user-specific settings without placing credentials in source control. */
public final class ApplicationConfiguration {
    private static final Path DEFAULT_PATH = Path.of("config", "application.properties");

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
