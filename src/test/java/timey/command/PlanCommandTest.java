package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class PlanCommandTest {
    @Test
    void constructor_locationsHaveSurroundingWhitespace_trimsLocations() {
        var command = new PlanCommand("  COM3  ", "  VivoCity  ", LocalTime.of(18, 30), Duration.ZERO);

        assertEquals("COM3", command.getOrigin());
        assertEquals("VivoCity", command.getDestination());
    }

    @Test
    void constructor_locationContainsControlCharacter_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new PlanCommand("COM3\tBlock", "VivoCity", LocalTime.of(18, 30), Duration.ZERO));

        assertEquals("Origin must not contain control characters.", exception.getMessage());
    }

    @Test
    void execute_plansRouteAndStoresTheCurrentPlan() {
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore());
        var command = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ofMinutes(5));

        var result = command.execute(model);

        assertEquals(command, model.getPendingPlan().orElseThrow());
        assertEquals(1, model.getPendingAlternatives().size());
        assertTrue(result.messages().contains("From: COM3"));
        assertTrue(result.messages().contains("Choose a route with: choose 1"));
    }
}
