package timey.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.command.PlanCommand;
import timey.domain.alert.SavedPlan;
import timey.domain.transit.RouteAlternative;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;
import timey.ports.PlanStore;

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
    void selectRoute_targetArrivalAlreadyPassed_schedulesTomorrowAndSavesPlan() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO).execute(model);

        RouteSelectionResult result = model.selectRoute(1);

        SavedPlan expected = new SavedPlan(LocalDate.of(2026, 8, 30), LocalTime.of(17, 0), "Admiralty MRT", "COM3",
                LocalTime.of(16, 17));
        assertEquals(RouteSelectionResult.Status.REMINDER_SCHEDULED, result.status());
        assertEquals(List.of(expected), model.getSavedPlans());
        assertEquals(List.of(List.of(expected)), savedPlanLists);
        assertEquals(Instant.parse("2026-08-30T08:17:00Z"), model.getScheduledReminders().getFirst().triggerAt());
    }

    @Test
    void selectRoute_leaveByAlreadyPassed_doesNotSavePlan() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T08:20:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO).execute(model);

        RouteSelectionResult result = model.selectRoute(1);

        assertEquals(RouteSelectionResult.Status.LEAVE_NOW, result.status());
        assertTrue(model.getSavedPlans().isEmpty());
        assertTrue(savedPlanLists.isEmpty());
    }

    @Test
    void selectRoute_departureCrossesMidnight_savesTargetArrivalDate() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T14:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(0, 10), Duration.ofMinutes(10)).execute(model);

        RouteSelectionResult result = model.selectRoute(1);

        SavedPlan expected = new SavedPlan(LocalDate.of(2026, 8, 30), LocalTime.of(0, 10), "Admiralty MRT", "COM3",
                LocalTime.of(23, 17));
        assertEquals(RouteSelectionResult.Status.REMINDER_SCHEDULED, result.status());
        assertEquals(List.of(expected), model.getSavedPlans());
        assertEquals(List.of(List.of(expected)), savedPlanLists);
        assertEquals(Instant.parse("2026-08-29T15:17:00Z"), model.getScheduledReminders().getFirst().triggerAt());
    }

    @Test
    void selectRoute_sameRouteSelectedTwice_rejectsSecondSelection() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO).execute(model);

        model.selectRoute(1);
        RouteSelectionResult secondResult = model.selectRoute(1);

        assertEquals(RouteSelectionResult.Status.ALREADY_SELECTED, secondResult.status());
        assertEquals(1, model.getSavedPlans().size());
        assertEquals(1, savedPlanLists.size());
        assertEquals(1, model.getScheduledReminders().size());
    }

    @Test
    void selectRoute_planHasNoAlternatives_reportsNoAlternatives() {
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore());
        var plan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ZERO);
        model.replacePlan(plan, List.of(), List.of("No routes found."));

        RouteSelectionResult result = model.selectRoute(1);

        assertEquals(RouteSelectionResult.Status.NO_ALTERNATIVES, result.status());
        assertTrue(model.getSelectedRecommendation().isEmpty());
    }

    @Test
    void replacePlan_afterRouteSelection_allowsSelectionFromFreshAlternatives() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock);
        var firstPlan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ZERO);
        var secondPlan = new PlanCommand("COM3", "HarbourFront", LocalTime.of(19, 0), Duration.ZERO);
        var firstRoute = new RouteAlternative("First route", Duration.ofMinutes(5), Duration.ofMinutes(30), 0);
        var secondRoute = new RouteAlternative("Second route", Duration.ofMinutes(5), Duration.ofMinutes(35), 0);
        model.replacePlan(firstPlan, List.of(firstRoute), List.of());
        model.selectRoute(1);

        model.replacePlan(secondPlan, List.of(secondRoute), List.of());
        RouteSelectionResult result = model.selectRoute(1);

        assertEquals(RouteSelectionResult.Status.REMINDER_SCHEDULED, result.status());
        assertEquals("Second route", result.recommendation().orElseThrow().routeName());
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
    void constructor_loadedDuplicatePlans_removesDuplicatesFromStore() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        SavedPlan future = new SavedPlan(LocalDate.of(2026, 8, 29), LocalTime.of(17, 0), "Home", "NUS",
                LocalTime.of(16, 0));
        PlanStore planStore = new PlanStore() {
            @Override
            public List<SavedPlan> loadAll() {
                return List.of(future, future);
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
