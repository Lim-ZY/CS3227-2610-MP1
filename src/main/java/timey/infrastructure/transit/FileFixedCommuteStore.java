package timey.infrastructure.transit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import timey.domain.transit.FixedCommute;
import timey.ports.FixedCommuteStore;

/** Properties-file implementation of locally persisted fixed commute timings. */
public final class FileFixedCommuteStore implements FixedCommuteStore {
    private final Path path;

    public FileFixedCommuteStore(Path path) {
        this.path = path;
    }

    @Override
    public synchronized void save(FixedCommute commute) {
        Properties timings = load();
        timings.setProperty(key(commute.origin(), commute.destination()), value(commute));
        save(timings);
    }

    private void save(Properties timings) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writeAtomically(timings);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save fixed commute timings.", exception);
        }
    }

    private void writeAtomically(Properties timings) throws IOException {
        Path temporaryFile = Files.createTempFile(path.toAbsolutePath().getParent(), "fixed-commutes-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporaryFile)) {
                timings.store(output, "Timey fixed commute timings");
            }
            moveIntoPlace(temporaryFile);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void moveIntoPlace(Path temporaryFile) throws IOException {
        try {
            Files.move(temporaryFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public synchronized Optional<FixedCommute> find(String origin, String destination) {
        String value = load().getProperty(key(origin, destination));
        if (value == null) {
            return Optional.empty();
        }
        try {
            long duration = duration(value);
            return Optional.of(new FixedCommute(origin, destination, Duration.ofMinutes(duration)));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.empty();
        }
    }

    @Override
    public synchronized List<FixedCommute> findAll() {
        Properties timings = load();
        return timings.stringPropertyNames().stream()
                .map(key -> fixedCommute(key, timings.getProperty(key)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(FixedCommute::origin, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(FixedCommute::destination, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public synchronized boolean remove(String origin, String destination) {
        Properties timings = load();
        if (timings.remove(key(origin, destination)) == null) {
            return false;
        }
        save(timings);
        return true;
    }

    private Properties load() {
        Properties timings = new Properties();
        if (!Files.isRegularFile(path)) {
            return timings;
        }
        try (InputStream input = Files.newInputStream(path)) {
            timings.load(input);
            return timings;
        } catch (IOException | IllegalArgumentException exception) {
            return timings;
        }
    }

    private Optional<FixedCommute> fixedCommute(String key, String value) {
        String[] locations = key.split("\\.", -1);
        if (locations.length != 2) {
            return Optional.empty();
        }
        try {
            String[] parts = value.split("\\|", -1);
            String origin = parts.length == 3 ? decode(parts[1]) : decode(locations[0]);
            String destination = parts.length == 3 ? decode(parts[2]) : decode(locations[1]);
            return Optional.of(new FixedCommute(origin, destination, Duration.ofMinutes(duration(value))));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.empty();
        }
    }

    private String key(String origin, String destination) {
        return encode(origin.trim().toLowerCase()) + "." + encode(destination.trim().toLowerCase());
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String value(FixedCommute commute) {
        return commute.duration().toMinutes() + "|" + encode(commute.origin()) + "|" + encode(commute.destination());
    }

    private long duration(String value) {
        return Long.parseLong(value.split("\\|", 2)[0]);
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
