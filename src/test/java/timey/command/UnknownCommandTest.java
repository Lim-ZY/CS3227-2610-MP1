package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class UnknownCommandTest {
    @Test
    void execute_returnsUsageGuidance() {
        var result = new UnknownCommand().execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals("Sorry I did not understand that... Use `help` for the list of commands I understand.",
                result.messages().getFirst());
    }
}
