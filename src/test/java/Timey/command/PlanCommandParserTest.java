package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class PlanCommandParserTest {
    private final PlanCommandParser parser = new PlanCommandParser();

    @Test
    void parsesCompletePlanCommand() {
        PlanCommand result = parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");

        assertEquals("COM3", result.origin());
        assertEquals("VivoCity", result.destination());
        assertEquals(LocalTime.of(18, 30), result.arrivalTime());
        assertEquals(Duration.ofMinutes(10), result.buffer());
    }

    @Test
    void usesDefaultBufferWhenItIsOmitted() {
        PlanCommand result = parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830");

        assertEquals(Duration.ofMinutes(10), result.buffer());
    }

    @Test
    void rejectsInvalidArrivalTime() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 2960"));

        assertEquals("Arrival time must be a valid 24-hour time.", exception.getMessage());
    }

    @Test
    void rejectsMissingDestination() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parser.parse("plan /from \"COM3\" /by 1830"));

        assertEquals("Missing required option /to.", exception.getMessage());
    }
}
