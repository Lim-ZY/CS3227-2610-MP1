package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import Timey.infrastructure.transit.InMemoryFixedCommuteStore;
import Timey.model.TimeyModel;

class AddCommandTest {
    @Test
    void execute_savesFixedCommuteAndReturnsFeedback() {
        var fixedCommutes = new InMemoryFixedCommuteStore();
        var command = new AddCommand("COM3", "VivoCity", Duration.ofMinutes(90));

        var result = command.execute(new TimeyModel(fixedCommutes));

        assertEquals(Duration.ofMinutes(90), fixedCommutes.find("COM3", "VivoCity").orElseThrow().duration());
        assertEquals("Saved fixed timing from COM3 to VivoCity: 90 minutes.", result.messages().getFirst());
    }
}
