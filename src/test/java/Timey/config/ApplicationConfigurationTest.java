package Timey.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;

class ApplicationConfigurationTest {
    @Test
    void readsOneMapTokenFromALocalPropertiesFile() throws Exception {
        var propertiesFile = Files.createTempFile("timey", ".properties");
        try {
            Files.writeString(propertiesFile, "onemap.access-token= local-token ");

            var configuration = ApplicationConfiguration.load(propertiesFile);

            assertEquals("local-token", configuration.oneMapAccessToken().orElseThrow());
        } finally {
            Files.deleteIfExists(propertiesFile);
        }
    }

    @Test
    void permitsAnAbsentLocalPropertiesFile() {
        assertDoesNotThrow(() -> ApplicationConfiguration.load(java.nio.file.Path.of("build", "missing.properties")));
    }
}
