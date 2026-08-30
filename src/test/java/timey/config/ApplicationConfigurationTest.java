package timey.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class ApplicationConfigurationTest {
    @Test
    void getLiveDataBaseUri_returnsApplicationOwnedEndpoint() {
        assertEquals(URI.create("https://cs3227-mp1-worker.tcmpiano03.workers.dev"),
                ApplicationConfiguration.getLiveDataBaseUri().orElseThrow());
    }

    @Test
    void getUserPreferences_returnsBuiltInDefaults() {
        UserPreferences preferences = ApplicationConfiguration.getUserPreferences();

        assertEquals(Duration.ofMinutes(10), preferences.defaultDepartureBuffer());
        assertEquals(List.of(), preferences.savedLocations());
    }
}
