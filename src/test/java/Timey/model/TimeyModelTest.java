package Timey.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import Timey.TestTimeyModelFactory;
import Timey.domain.transit.RouteAlternative;
import Timey.domain.alert.SavedPlan;
import Timey.ports.PlanStore;
import Timey.infrastructure.transit.InMemoryFixedCommuteStore;
import Timey.command.PlanCommand;

class TimeyModelTest {
    @Test
    void replacePlan_storesIndependentPlanningState() {
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore());
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ofMinutes(5));
        var alternatives = List.of(new RouteAlternative("Bus", Duration.ofMinutes(5), Duration.ofMinutes(30), 0));

        model.replacePlan(plan, alternatives, List.of("Offline route available."));
        model.addPlanningMessage("Saved timing available.");

        assertEquals(plan, model.getPendingPlan().orElseThrow());
        assertEquals(alternatives, model.getPendingAlternatives());
        assertEquals(List.of("Offline route available.", "Saved timing available."), model.getPlanningMessages());
        assertTrue(model.getSelectedRecommendation().isEmpty());
    }

    @Test
    void selectRoute_targetArrivalStillToday_savesSelectedPlanToStore() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO).execute(model);

        model.selectRoute(1);

        SavedPlan expected = new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Admiralty MRT", "COM3",
                LocalTime.of(16, 17));
        assertEquals(List.of(expected), model.getSavedPlans());
        assertEquals(List.of(List.of(expected)), savedPlanLists);
    }

    @Test
    void selectRoute_targetArrivalAlreadyPassed_doesNotSavePlan() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO).execute(model);

        model.selectRoute(1);

        assertTrue(model.getSavedPlans().isEmpty());
        assertTrue(savedPlanLists.isEmpty());
    }

    @Test
    void selectRoute_leaveByAlreadyPassed_doesNotSavePlan() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T08:20:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO).execute(model);

        model.selectRoute(1);

        assertTrue(model.getSavedPlans().isEmpty());
        assertTrue(savedPlanLists.isEmpty());
    }

    @Test
    void constructor_loadedPlansContainExpiredEntry_removesItFromStore() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        SavedPlan expired = new SavedPlan(LocalDate.of(2026, 8, 28), LocalTime.of(17, 0), "Home", "NUS",
                LocalTime.of(16, 0));
        SavedPlan future = new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Home", "NUS",
                LocalTime.of(16, 0));
        PlanStore planStore = new PlanStore() {
            @Override
            public List<SavedPlan> loadAll() {
                return List.of(expired, future);
            }

            @Override
            public void saveAll(List<SavedPlan> plans) {
                savedPlanLists.add(List.copyOf(plans));
            }
        };

        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock, planStore);

        assertEquals(List.of(future), model.getSavedPlans());
        assertEquals(List.of(List.of(future)), savedPlanLists);
    }

    @Test
    void constructor_loadedPlanWithPassedLeaveBy_removesItFromStore() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T08:20:00Z"), ZoneId.of("Asia/Singapore"));
        SavedPlan expired = new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Home", "NUS",
                LocalTime.of(16, 0));
        PlanStore planStore = new PlanStore() {
            @Override
            public List<SavedPlan> loadAll() {
                return List.of(expired);
            }

            @Override
            public void saveAll(List<SavedPlan> plans) {
                savedPlanLists.add(List.copyOf(plans));
            }
        };

        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock, planStore);

        assertTrue(model.getSavedPlans().isEmpty());
        assertEquals(List.of(List.of()), savedPlanLists);
    }

    @Test
    void close_planExpiresDuringSession_removesItFromStore() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        var clock = new MutableClock(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        SavedPlan future = new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Home", "NUS",
                LocalTime.of(16, 0));
        PlanStore planStore = new PlanStore() {
            @Override
            public List<SavedPlan> loadAll() {
                return List.of(future);
            }

            @Override
            public void saveAll(List<SavedPlan> plans) {
                savedPlanLists.add(List.copyOf(plans));
            }
        };
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock, planStore);

        clock.setInstant(Instant.parse("2026-08-29T12:00:00Z"));
        model.close();

        assertTrue(model.getSavedPlans().isEmpty());
        assertEquals(List.of(List.of()), savedPlanLists);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId newZone) {
            return new MutableClock(instant, newZone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
