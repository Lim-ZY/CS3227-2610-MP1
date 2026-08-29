package timey.infrastructure.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import timey.domain.alert.SavedPlan;

class FilePlanStoreTest {
    @Test
    void saveAll_missingParentDirectory_writesFormattedPlans() throws Exception {
        var directory = Files.createTempDirectory("timey-plans");
        try {
            var path = directory.resolve("nested").resolve("plans.txt");
            var store = new FilePlanStore(path);

            store.saveAll(List.of(
                    new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Admiralty MRT", "COM3",
                            LocalTime.of(15, 50)),
                    new SavedPlan(LocalDate.of(2026, 8, 30), LocalTime.of(9, 5), "Home", "NUS",
                            LocalTime.of(8, 20))));

            assertTrue(Files.isRegularFile(path));
            assertEquals("29-08-2026 | 1700 | Admiralty MRT -> COM3 | leave by 1550\n"
                    + "30-08-2026 | 0905 | Home -> NUS | leave by 0820\n", Files.readString(path));
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void saveAll_existingFile_replacesItsContents() throws Exception {
        var directory = Files.createTempDirectory("timey-plans");
        try {
            var path = directory.resolve("plans.txt");
            Files.writeString(path, "old plan\n");

            new FilePlanStore(path).saveAll(List.of());

            assertEquals("", Files.readString(path));
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void loadAll_formattedPlans_returnsSavedPlansInFileOrder() throws Exception {
        var directory = Files.createTempDirectory("timey-plans");
        try {
            var path = directory.resolve("plans.txt");
            Files.writeString(path, "29-08-2026 | 1700 | Admiralty MRT -> COM3 | leave by 1550\n"
                    + "30-08-2026 | 0905 | Home -> NUS | leave by 0820\n");

            var result = new FilePlanStore(path).loadAll();

            assertEquals(List.of(
                    new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Admiralty MRT", "COM3",
                            LocalTime.of(15, 50)),
                    new SavedPlan(LocalDate.of(2026, 8, 30), LocalTime.of(9, 5), "Home", "NUS",
                            LocalTime.of(8, 20))), result);
        } finally {
            deleteDirectory(directory);
        }
    }

    @Test
    void loadAll_malformedAndBlankLines_keepsValidPlans() throws Exception {
        var directory = Files.createTempDirectory("timey-plans");
        try {
            var path = directory.resolve("plans.txt");
            Files.writeString(path, "not a plan\n\n29-08-2026 | 1700 | Admiralty MRT -> COM3 | leave by 1550\n"
                    + "30-02-2026 | 0900 | Home -> NUS | leave by 0800\n");

            var result = new FilePlanStore(path).loadAll();

            assertEquals(List.of(new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Admiralty MRT",
                    "COM3", LocalTime.of(15, 50))), result);
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
