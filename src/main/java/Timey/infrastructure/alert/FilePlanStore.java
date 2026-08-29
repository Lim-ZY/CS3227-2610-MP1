package Timey.infrastructure.alert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import Timey.domain.alert.SavedPlan;
import Timey.ports.PlanStore;

/** Text-file implementation of locally persisted selected commute plans. */
public final class FilePlanStore implements PlanStore {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    private final Path path;

    public FilePlanStore(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public synchronized void saveAll(List<SavedPlan> plans) {
        Objects.requireNonNull(plans);
        String content = plans.stream()
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
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save plans.", exception);
        }
    }

    private String format(SavedPlan plan) {
        return DATE_FORMAT.format(plan.date()) + " | " + TIME_FORMAT.format(plan.arrivalTime())
                + " | " + plan.origin() + " -> " + plan.destination()
                + " | leave by " + TIME_FORMAT.format(plan.leaveBy());
    }
}
