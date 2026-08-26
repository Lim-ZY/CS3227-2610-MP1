package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import Timey.TestTimeyModelFactory;
import Timey.domain.transit.FixedCommute;
import Timey.infrastructure.transit.InMemoryFixedCommuteStore;

class ListFixedTimingsCommandTest {
    @Test
    void execute_savedTimings_listsTimingsInStableOrder() {
        var store = new InMemoryFixedCommuteStore();
        store.save(new FixedCommute("VivoCity", "COM3", Duration.ofMinutes(90)));
        store.save(new FixedCommute("COM3", "Home", Duration.ofMinutes(25)));

        var result = new ListFixedTimingsCommand().execute(TestTimeyModelFactory.create(store));

        assertEquals("Saved fixed timings:", result.messages().getFirst());
        assertEquals("1. COM3 → Home — 25 minutes", result.messages().get(1));
        assertEquals("2. VivoCity → COM3 — 90 minutes", result.messages().get(2));
    }

    @Test
    void execute_noSavedTimings_displaysEmptyMessage() {
        var result = new ListFixedTimingsCommand().execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals("You have no saved fixed timings.", result.messages().getFirst());
    }
}
