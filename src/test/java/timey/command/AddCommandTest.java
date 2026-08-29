package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class AddCommandTest {
    @Test
    void execute_savesFixedCommuteAndReturnsFeedback() {
        var fixedCommutes = new InMemoryFixedCommuteStore();
        var command = new AddCommand("COM3", "VivoCity", Duration.ofMinutes(90));

        var result = command.execute(TestTimeyModelFactory.create(fixedCommutes));

        assertEquals(Duration.ofMinutes(90), fixedCommutes.find("COM3", "VivoCity").orElseThrow().duration());
        assertEquals("Saved fixed timing from COM3 to VivoCity: 90 minutes.", result.messages().getFirst());
    }
}
