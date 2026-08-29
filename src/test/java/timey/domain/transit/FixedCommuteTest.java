package timey.domain.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class FixedCommuteTest {
    @Test
    void constructor_locationsHaveSurroundingWhitespace_trimsLocations() {
        var commute = new FixedCommute("  COM3  ", "  VivoCity  ", Duration.ofMinutes(90));

        assertEquals("COM3", commute.origin());
        assertEquals("VivoCity", commute.destination());
    }

    @Test
    void constructor_locationContainsControlCharacter_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new FixedCommute("COM3\tBlock", "VivoCity", Duration.ofMinutes(90)));

        assertEquals("Origin must not contain control characters.", exception.getMessage());
    }

    @Test
    void constructor_zeroDuration_validationErrorThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new FixedCommute("COM3", "VivoCity", Duration.ZERO));

        assertEquals("Fixed duration must be greater than zero.", exception.getMessage());
    }
}
