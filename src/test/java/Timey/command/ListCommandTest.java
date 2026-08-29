package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import Timey.TestTimeyModelFactory;
import Timey.domain.alert.SavedPlan;
import Timey.domain.transit.FixedCommute;
import Timey.infrastructure.transit.InMemoryFixedCommuteStore;
import Timey.ports.PlanStore;

class ListCommandTest {
    @Test
    void execute_savedTimings_listsTimingsInStableOrder() {
        var store = new InMemoryFixedCommuteStore();
        store.save(new FixedCommute("VivoCity", "COM3", Duration.ofMinutes(90)));
        store.save(new FixedCommute("COM3", "Home", Duration.ofMinutes(25)));

        var result = new ListCommand(ListCommand.ListType.SAVED).execute(TestTimeyModelFactory.create(store));

        assertEquals("Saved timings:", result.messages().getFirst());
        assertEquals("1. COM3 → Home — 25 minutes", result.messages().get(1));
        assertEquals("2. VivoCity → COM3 — 90 minutes", result.messages().get(2));
    }

    @Test
    void execute_noSavedTimings_displaysEmptyMessage() {
        var result = new ListCommand(ListCommand.ListType.SAVED)
                .execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals("You have no saved timings.", result.messages().getFirst());
    }

    @Test
    void execute_savedPlans_listsFuturePlans() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        SavedPlan plan = new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Home", "NUS",
                LocalTime.of(16, 0));
        PlanStore planStore = new PlanStore() {
            @Override
            public List<SavedPlan> loadAll() {
                return List.of(plan);
            }

            @Override
            public void saveAll(List<SavedPlan> plans) {
            }
        };

        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock, planStore);
        var result = new ListCommand(ListCommand.ListType.PLANS).execute(model);

        assertEquals(List.of("Saved plans:", "1. 29-08-2026 | 1700 | Home → NUS | leave by 1600"),
                result.messages());
    }

    @Test
    void execute_noFutureSavedPlans_displaysEmptyMessage() {
        var result = new ListCommand(ListCommand.ListType.PLANS)
                .execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals("You have no saved plans.", result.messages().getFirst());
    }
}
