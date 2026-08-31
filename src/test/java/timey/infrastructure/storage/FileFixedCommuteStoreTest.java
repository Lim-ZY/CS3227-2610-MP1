package timey.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import timey.domain.transit.FixedCommute;

class FileFixedCommuteStoreTest {
    @Test
    void save_newStoreReadsSameLocationPair_fixedTimingReturned() throws Exception {
        var directory = Files.createTempDirectory("timey-fixed-commute");
        try {
            var path = directory.resolve("fixed-commutes.txt");
            var store = new FileFixedCommuteStore(path);
            store.save(new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(90)));
            store.save(new FixedCommute("Home", "COM3", Duration.ofMinutes(25)));

            assertEquals("COM3 -> VivoCity = 90m" + System.lineSeparator()
                    + "Home -> COM3 = 25m" + System.lineSeparator(), Files.readString(path));

            var result = new FileFixedCommuteStore(path).find("com3", "vivocity");

            assertTrue(result.isPresent());
            assertEquals(Duration.ofMinutes(90), result.orElseThrow().duration());
            assertEquals("COM3", store.findAll().getFirst().origin());
            assertTrue(store.remove("COM3", "VivoCity"));
            assertTrue(store.find("COM3", "VivoCity").isEmpty());
        } finally {
            Files.walk(directory).sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }
    }

    @Test
    void save_caseOrWhitespaceVariant_replacesExistingTiming() throws Exception {
        var directory = Files.createTempDirectory("timey-fixed-commute");
        try {
            var path = directory.resolve("fixed-commutes.txt");
            var store = new FileFixedCommuteStore(path);
            store.save(new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(90)));
            store.save(new FixedCommute("  com3  ", "  vivocity  ", Duration.ofMinutes(75)));

            assertEquals(1, store.findAll().size());
            assertEquals(Duration.ofMinutes(75), store.find("COM3", "VivoCity").orElseThrow().duration());
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void findAll_malformedTextFile_returnsEmptyList() throws Exception {
        var directory = Files.createTempDirectory("timey-fixed-commute");
        try {
            var path = directory.resolve("fixed-commutes.txt");
            Files.writeString(path, "malformed line\nCOM3 -> VivoCity = nope\n");

            assertEquals(java.util.List.of(), new FileFixedCommuteStore(path).findAll());
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void save_malformedExistingFile_recoversWithReplacementFile() throws Exception {
        var directory = Files.createTempDirectory("timey-fixed-commute");
        try {
            var path = directory.resolve("fixed-commutes.txt");
            Files.writeString(path, "malformed line\n");
            var expected = new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(75));

            new FileFixedCommuteStore(path).save(expected);

            assertEquals(java.util.List.of(expected), new FileFixedCommuteStore(path).findAll());
            try (var files = Files.list(directory)) {
                assertEquals(1, files.count());
            }
        } finally {
            deleteDirectory(directory);
        }
    }

    private void deleteDirectory(java.nio.file.Path directory) throws Exception {
        Files.walk(directory).sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }
}
