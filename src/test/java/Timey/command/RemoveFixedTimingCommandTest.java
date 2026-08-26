package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import Timey.TestTimeyModelFactory;
import Timey.domain.transit.FixedCommute;
import Timey.infrastructure.transit.InMemoryFixedCommuteStore;

class RemoveFixedTimingCommandTest {
    @Test
    void execute_existingListNumber_removesSavedTiming() {
        var store = new InMemoryFixedCommuteStore();
        store.save(new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(90)));

        var result = new RemoveFixedTimingCommand(1).execute(TestTimeyModelFactory.create(store));

        assertEquals("Removed fixed timing from COM3 to VivoCity.", result.messages().getFirst());
        assertEquals(0, store.findAll().size());
    }

    @Test
    void execute_missingOrInvalidListNumber_displaysGuidanceWithoutDeleting() {
        var store = new InMemoryFixedCommuteStore();
        store.save(new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(90)));
        var model = TestTimeyModelFactory.create(store);

        assertEquals("Remove a saved timing by number, for example: rm 1",
                new RemoveFixedTimingCommand(null).execute(model).messages().getFirst());
        assertEquals("No saved fixed timing numbered 2.",
                new RemoveFixedTimingCommand(2).execute(model).messages().getFirst());
        assertEquals(1, store.findAll().size());
    }
}
