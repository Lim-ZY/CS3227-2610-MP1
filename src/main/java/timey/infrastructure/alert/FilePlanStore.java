package timey.infrastructure.alert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import timey.domain.alert.SavedPlan;
import timey.ports.PlanStore;

/** Text-file implementation of locally persisted selected commute plans. */
public final class FilePlanStore implements PlanStore {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    private final Path path;

    public FilePlanStore(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public synchronized void saveAll(List<SavedPlan> plans) {
        Objects.requireNonNull(plans);
        List<SavedPlan> plansToSave = List.copyOf(plans);
        String content = plansToSave.stream()
                .map(this::format)
                .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
        if (!content.isEmpty()) {
            content += System.lineSeparator();
        }
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporaryFile = Files.createTempFile(path.toAbsolutePath().getParent(), "plans-", ".tmp");
            try {
                Files.writeString(temporaryFile, content, StandardCharsets.UTF_8);
                moveIntoPlace(temporaryFile);
            } finally {
                Files.deleteIfExists(temporaryFile);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save plans.", exception);
        }
    }

    @Override
    public synchronized List<SavedPlan> loadAll() {
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        try {
            List<SavedPlan> plans = new ArrayList<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    parseSafely(line).ifPresent(plans::add);
                }
            }
            return List.copyOf(plans);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load plans.", exception);
        }
    }

    private void moveIntoPlace(Path temporaryFile) throws IOException {
        try {
            Files.move(temporaryFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private java.util.Optional<SavedPlan> parseSafely(String line) {
        try {
            return java.util.Optional.of(parse(line));
        } catch (java.time.DateTimeException | IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private String format(SavedPlan plan) {
        return DATE_FORMAT.format(plan.date()) + " | " + TIME_FORMAT.format(plan.arrivalTime())
                + " | " + plan.origin() + " -> " + plan.destination()
                + " | leave by " + TIME_FORMAT.format(plan.leaveBy());
    }

    private SavedPlan parse(String line) {
        String[] parts = line.split(" \\| ", -1);
        if (parts.length != 4 || !parts[3].startsWith("leave by ")) {
            throw new IllegalArgumentException("Plan line is not in the expected format.");
        }
        String[] locations = parts[2].split(" -> ", -1);
        if (locations.length != 2) {
            throw new IllegalArgumentException("Plan line does not contain a valid journey.");
        }
        return new SavedPlan(java.time.LocalDate.parse(parts[0], DATE_FORMAT),
                java.time.LocalTime.parse(parts[1], TIME_FORMAT), locations[0], locations[1],
                java.time.LocalTime.parse(parts[3].substring("leave by ".length()), TIME_FORMAT));
    }
}
