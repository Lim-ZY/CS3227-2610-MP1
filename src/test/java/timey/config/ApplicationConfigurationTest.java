package timey.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class ApplicationConfigurationTest {
    @Test
    void load_liveDataUrl_isApplicationOwned() throws Exception {
        var propertiesFile = Files.createTempFile("timey", ".properties");
        try {
            Files.writeString(propertiesFile, "timey.live-data-url=https://ignored.example");

            var configuration = ApplicationConfiguration.load(propertiesFile);

            assertEquals(java.net.URI.create("https://cs3227-mp1-worker.tcmpiano03.workers.dev"),
                    configuration.getLiveDataBaseUri().orElseThrow());
        } finally {
            Files.deleteIfExists(propertiesFile);
        }
    }

    @Test
    void load_preferencesDoNotDisableLiveData() throws Exception {
        var propertiesFile = Files.createTempFile("timey", ".properties");
        try {
            Files.writeString(propertiesFile, "timey.live-data-url=http://localhost:8787");

            assertEquals(java.net.URI.create("https://cs3227-mp1-worker.tcmpiano03.workers.dev"),
                    ApplicationConfiguration.load(propertiesFile).getLiveDataBaseUri().orElseThrow());
        } finally {
            Files.deleteIfExists(propertiesFile);
        }
    }

    @Test
    void load_propertiesFileAbsent_configurationReturned() {
        assertDoesNotThrow(() -> ApplicationConfiguration.load(java.nio.file.Path.of("build", "missing.properties")));
    }

    @Test
    void load_preferenceProperties_preferencesReturned() throws Exception {
        var propertiesFile = Files.createTempFile("timey", ".properties");
        try {
            Files.writeString(propertiesFile, "timezone=Europe/London\n"
                    + "departure-buffer-minutes=15\n"
                    + "saved-locations=COM3, Home, COM3\n");

            var preferences = ApplicationConfiguration.load(propertiesFile).getUserPreferences();

            assertEquals(Duration.ofMinutes(15), preferences.defaultDepartureBuffer());
            assertEquals(java.util.List.of("COM3", "Home"), preferences.savedLocations());
        } finally {
            Files.deleteIfExists(propertiesFile);
        }
    }

    @Test
    void load_invalidPreferenceProperties_safeDefaultsReturned() throws Exception {
        var propertiesFile = Files.createTempFile("timey", ".properties");
        try {
            Files.writeString(propertiesFile, "timezone=Not/AZone\ndeparture-buffer-minutes=-2\n");

            var preferences = ApplicationConfiguration.load(propertiesFile).getUserPreferences();

            assertEquals(Duration.ofMinutes(10), preferences.defaultDepartureBuffer());
            assertEquals(java.util.List.of(), preferences.savedLocations());
        } finally {
            Files.deleteIfExists(propertiesFile);
        }
    }

    @Test
    void load_malformedPropertiesFile_safeDefaultsReturned() throws Exception {
        var propertiesFile = Files.createTempFile("timey", ".properties");
        try {
            Files.writeString(propertiesFile, "departure-buffer-minutes=20\nmalformed=\\u12G4");

            var preferences = ApplicationConfiguration.load(propertiesFile).getUserPreferences();

            assertEquals(Duration.ofMinutes(10), preferences.defaultDepartureBuffer());
            assertEquals(java.util.List.of(), preferences.savedLocations());
        } finally {
            Files.deleteIfExists(propertiesFile);
        }
    }

    @Test
    void load_oversizedOrDuplicatePreferences_usesSafeValues() throws Exception {
        var propertiesFile = Files.createTempFile("timey", ".properties");
        try {
            Files.writeString(propertiesFile, "departure-buffer-minutes=721\n"
                    + "saved-locations=COM3, com3, Home, " + "x".repeat(201) + ", Bad\u0001Location");

            var preferences = ApplicationConfiguration.load(propertiesFile).getUserPreferences();

            assertEquals(Duration.ofMinutes(10), preferences.defaultDepartureBuffer());
            assertEquals(java.util.List.of("COM3", "Home"), preferences.savedLocations());
        } finally {
            Files.deleteIfExists(propertiesFile);
        }
    }
}
