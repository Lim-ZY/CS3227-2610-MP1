package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.domain.transit.FixedCommute;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class RemoveCommandTest {
    @Test
    void execute_existingListNumber_removesSavedTiming() {
        var store = new InMemoryFixedCommuteStore();
        store.save(new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(90)));

        var result = new RemoveCommand(1).execute(TestTimeyModelFactory.create(store));

        assertEquals("Removed saved timing from COM3 to VivoCity.", result.messages().getFirst());
        assertEquals(0, store.findAll().size());
    }

    @Test
    void execute_missingOrInvalidListNumber_displaysGuidanceWithoutDeleting() {
        var store = new InMemoryFixedCommuteStore();
        store.save(new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(90)));
        var model = TestTimeyModelFactory.create(store);

        assertEquals("Remove a saved timing by number, for example: rm 1",
                new RemoveCommand(null).execute(model).messages().getFirst());
        assertEquals("No saved timing numbered 2.",
                new RemoveCommand(2).execute(model).messages().getFirst());
        assertEquals(1, store.findAll().size());
    }

    @Test
    void execute_secondSortedTiming_removesOnlyThatTiming() {
        var store = new InMemoryFixedCommuteStore();
        store.save(new FixedCommute("COM3", "VivoCity", Duration.ofMinutes(90)));
        store.save(new FixedCommute("Home", "COM3", Duration.ofMinutes(25)));
        var model = TestTimeyModelFactory.create(store);

        new RemoveCommand(2).execute(model);

        assertEquals(java.util.List.of("COM3"), store.findAll().stream().map(FixedCommute::origin).toList());
    }
}
