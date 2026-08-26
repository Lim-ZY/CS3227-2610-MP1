package Timey.infrastructure.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import Timey.domain.transit.FixedCommute;

class FileFixedCommuteStoreTest {
    @Test
    void save_newStoreReadsSameLocationPair_fixedTimingReturned() throws Exception {
        var directory = Files.createTempDirectory("timey-fixed-commute");
        try {
            var path = directory.resolve("fixed-commutes.properties");
            new FileFixedCommuteStore(path).save(new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(90)));

            var result = new FileFixedCommuteStore(path).find("com3", "vivocity");

            assertTrue(result.isPresent());
            assertEquals(Duration.ofMinutes(90), result.orElseThrow().duration());
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
}
