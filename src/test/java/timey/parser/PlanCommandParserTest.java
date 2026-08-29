package timey.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import timey.command.PlanCommand;

class PlanCommandParserTest {
    private final PlanCommandParser parser = new PlanCommandParser();

    @Test
    void parse_completeCommand_planCreated() {
        PlanCommand result = parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m");

        assertEquals("COM3", result.getOrigin());
        assertEquals("VivoCity", result.getDestination());
        assertEquals(LocalTime.of(18, 30), result.getArrivalTime());
        assertEquals(Duration.ofMinutes(10), result.getBuffer());
    }

    @Test
    void parse_bufferOmitted_defaultBufferUsed() {
        PlanCommand result = parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830");

        assertEquals(Duration.ofMinutes(10), result.getBuffer());
    }

    @Test
    void parse_bufferOmitted_configuredDefaultBufferUsed() {
        PlanCommand result = new PlanCommandParser(Duration.ofMinutes(15))
                .parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830");

        assertEquals(Duration.ofMinutes(15), result.getBuffer());
    }

    @Test
    void parse_zeroBuffer_zeroBufferAccepted() {
        PlanCommand result = parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 0m");

        assertEquals(Duration.ZERO, result.getBuffer());
    }

    @Test
    void parse_invalidArrivalTime_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 2960"));

        assertEquals("Arrival time must be a valid 24-hour time.", exception.getMessage());
    }

    @Test
    void parse_malformedArrivalOrBuffer_validationErrorThrown() {
        assertEquals("Arrival time must use 24-hour HHmm format, for example 1830.",
                assertThrows(IllegalArgumentException.class, () ->
                        parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 18:30")).getMessage());
        assertEquals("Buffer must be a whole number of minutes, for example 10m.",
                assertThrows(IllegalArgumentException.class, () ->
                        parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf -1m")).getMessage());
        assertEquals("Buffer must be a whole number of minutes, for example 10m.",
                assertThrows(IllegalArgumentException.class, () ->
                        parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 1.5m")).getMessage());
    }

    @Test
    void parse_destinationMissing_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                parser.parse("plan /from \"COM3\" /by 1830"));

        assertEquals("Missing required option /to.", exception.getMessage());
    }

    @Test
    void parse_duplicateOptionOrUnterminatedLocation_validationErrorThrown() {
        assertEquals("Option /from was provided more than once.",
                assertThrows(IllegalArgumentException.class, () -> parser.parse(
                        "plan /from \"COM3\" /from \"Home\" /to \"VivoCity\" /by 1830")).getMessage());
        assertEquals("Could not understand part of the plan command.",
                assertThrows(IllegalArgumentException.class, () ->
                        parser.parse("plan /from \"COM3 /to \"VivoCity\" /by 1830")).getMessage());
    }

    @Test
    void parse_oversizedBuffer_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parser.parse(
                "plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 999999999999999999999999999m"));

        assertEquals("Buffer is too large.", exception.getMessage());
    }
}
