package Timey.infrastructure.transit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;

import Timey.domain.transit.FixedCommute;
import Timey.ports.FixedCommuteStore;

/** Properties-file implementation of locally persisted fixed commute timings. */
public final class FileFixedCommuteStore implements FixedCommuteStore {
    private final Path path;

    public FileFixedCommuteStore(Path path) {
        this.path = path;
    }

    @Override
    public synchronized void save(FixedCommute commute) {
        Properties timings = load();
        timings.setProperty(key(commute.origin(), commute.destination()), Long.toString(commute.duration().toMinutes()));
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream output = Files.newOutputStream(path)) {
                timings.store(output, "Timey fixed commute timings");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save fixed commute timings.", exception);
        }
    }

    @Override
    public synchronized Optional<FixedCommute> find(String origin, String destination) {
        String minutes = load().getProperty(key(origin, destination));
        if (minutes == null) {
            return Optional.empty();
        }
        try {
            long duration = Long.parseLong(minutes);
            return Optional.of(new FixedCommute(origin, destination, Duration.ofMinutes(duration)));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.empty();
        }
    }

    private Properties load() {
        Properties timings = new Properties();
        if (!Files.isRegularFile(path)) {
            return timings;
        }
        try (InputStream input = Files.newInputStream(path)) {
            timings.load(input);
            return timings;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load fixed commute timings.", exception);
        }
    }

    private String key(String origin, String destination) {
        return encode(origin.trim().toLowerCase()) + "." + encode(destination.trim().toLowerCase());
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
