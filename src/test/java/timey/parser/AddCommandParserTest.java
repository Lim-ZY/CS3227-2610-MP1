package timey.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import timey.command.AddCommand;

class AddCommandParserTest {
    private final AddCommandParser parser = new AddCommandParser();

    @Test
    void parse_hoursAndMinutes_addTimingCreated() {
        AddCommand command = parser.parse("add /from \"COM3\" /to \"VivoCity\" /dur 1h30m");

        assertEquals("COM3", command.getOrigin());
        assertEquals("VivoCity", command.getDestination());
        assertEquals(Duration.ofMinutes(90), command.getDuration());
    }

    @Test
    void parse_hourOrMinuteDuration_addTimingCreated() {
        assertEquals(Duration.ofHours(1), parser.parse("add /from A /to B /dur 1h").getDuration());
        assertEquals(Duration.ofMinutes(30), parser.parse("add /from A /to B /dur 30m").getDuration());
    }

    @Test
    void parse_invalidDuration_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                parser.parse("add /from A /to B /dur 90"));

        assertEquals("Duration must use hours and minutes, for example 1h30m.", exception.getMessage());
    }

    @Test
    void parse_zeroDuration_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                parser.parse("add /from A /to B /dur 0m"));

        assertEquals("Duration must be greater than zero.", exception.getMessage());
    }
}
