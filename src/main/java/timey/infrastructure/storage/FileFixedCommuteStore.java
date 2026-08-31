package timey.infrastructure.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import timey.domain.transit.FixedCommute;
import timey.ports.FixedCommuteStore;

/** Text-file implementation of locally persisted fixed commute timings. */
public final class FileFixedCommuteStore implements FixedCommuteStore {
    private final Path path;

    public FileFixedCommuteStore(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    @Override
    public synchronized void save(FixedCommute commute) {
        List<FixedCommute> commutes = findAll().stream()
                .filter(saved -> !sameJourney(saved, commute))
                .collect(Collectors.toCollection(ArrayList::new));
        commutes.add(commute);
        write(commutes);
    }

    @Override
    public synchronized Optional<FixedCommute> find(String origin, String destination) {
        return findAll().stream()
                .filter(commute -> sameJourney(commute, origin, destination))
                .findFirst();
    }

    @Override
    public synchronized List<FixedCommute> findAll() {
        if (!Files.isRegularFile(path)) {
            return List.of();
        }
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
                    .filter(line -> !line.isBlank())
                    .map(this::parseSafely)
                    .flatMap(Optional::stream)
                    .sorted(Comparator.comparing(FixedCommute::origin, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(FixedCommute::destination, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load fixed commute timings.", exception);
        }
    }

    @Override
    public synchronized boolean remove(String origin, String destination) {
        List<FixedCommute> commutes = findAll();
        List<FixedCommute> remaining = commutes.stream()
                .filter(commute -> !sameJourney(commute, origin, destination))
                .toList();
        if (remaining.size() == commutes.size()) {
            return false;
        }
        write(remaining);
        return true;
    }

    private void write(List<FixedCommute> commutes) {
        String content = commutes.stream()
                .sorted(Comparator.comparing(FixedCommute::origin, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(FixedCommute::destination, String.CASE_INSENSITIVE_ORDER))
                .map(this::format)
                .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
        try {
            AtomicFileWriter.write(path, "fixed-commutes-", temporaryFile ->
                    Files.writeString(temporaryFile, content, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save fixed commute timings.", exception);
        }
    }

    private Optional<FixedCommute> parseSafely(String line) {
        try {
            return Optional.of(parse(line));
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return Optional.empty();
        }
    }

    private FixedCommute parse(String line) {
        String[] journeyAndDuration = line.split(" = ", -1);
        if (journeyAndDuration.length != 2 || !journeyAndDuration[1].endsWith("m")) {
            throw new IllegalArgumentException("Fixed commute line is not in the expected format.");
        }
        String[] locations = journeyAndDuration[0].split(" -> ", -1);
        if (locations.length != 2) {
            throw new IllegalArgumentException("Fixed commute line does not contain a valid journey.");
        }
        long minutes = Long.parseLong(journeyAndDuration[1].substring(0, journeyAndDuration[1].length() - 1));
        return new FixedCommute(locations[0], locations[1], Duration.ofMinutes(minutes));
    }

    private String format(FixedCommute commute) {
        return commute.origin() + " -> " + commute.destination() + " = " + commute.duration().toMinutes() + "m";
    }

    private boolean sameJourney(FixedCommute first, FixedCommute second) {
        return sameJourney(first, second.origin(), second.destination());
    }

    private boolean sameJourney(FixedCommute commute, String origin, String destination) {
        return commute.origin().equalsIgnoreCase(origin.strip())
                && commute.destination().equalsIgnoreCase(destination.strip());
    }
}
