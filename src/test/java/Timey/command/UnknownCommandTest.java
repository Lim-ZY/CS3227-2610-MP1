package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import Timey.TestTimeyModelFactory;
import Timey.infrastructure.transit.InMemoryFixedCommuteStore;

class UnknownCommandTest {
    @Test
    void execute_returnsUsageGuidance() {
        var result = new UnknownCommand().execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals("I did not understand that. Try: plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m",
                result.messages().getFirst());
    }
}
