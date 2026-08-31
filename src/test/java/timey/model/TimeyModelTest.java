package timey.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.command.PlanCommand;
import timey.domain.alert.SavedPlan;
import timey.domain.location.LocationResolution;
import timey.domain.location.ResolvedLocation;
import timey.domain.transit.FixedCommute;
import timey.domain.transit.LiveRouteLookup;
import timey.domain.transit.RouteAlternative;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;
import timey.infrastructure.transit.MockTransitPlanner;
import timey.planner.CommutePlanningService;
import timey.planner.Planner;
import timey.ports.FixedCommuteStore;
import timey.ports.LiveTransitPlanner;
import timey.ports.LocationResolver;
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
    void plan_locationFailure_clearsPreviouslyUnchosenPlan() {
        var previousPlan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ZERO);
        var previousRoute = new RouteAlternative("Previous route", Duration.ofMinutes(5), Duration.ofMinutes(30), 0);
        Clock clock = Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore"));
        LiveTransitPlanner liveTransitPlanner = (origin, destination, date, time) ->
                LiveRouteLookup.available(List.of());
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()),
                query -> LocationResolution.unavailable(400, "OneMap could not find \"" + query + "\"."),
                liveTransitPlanner, clock);
        var model = new TimeyModel(planner, new InMemoryFixedCommuteStore(), plans -> { }, clock);
        model.replacePlan(previousPlan, List.of(previousRoute), List.of("Previous route is available."));

        model.plan(new PlanCommand("Home", "NUS", LocalTime.of(9, 0), Duration.ZERO));

        assertTrue(model.getPendingPlan().isEmpty());
        assertTrue(model.getPendingAlternatives().isEmpty());
        assertTrue(model.getSelectedRecommendation().isEmpty());
        assertFalse(model.isUsingFallbackEstimate());
        assertEquals(List.of("I'm so sorry, OneMap could not find \"Home\".",
                "Perhaps you can give me the postal code for that location instead?"), model.getPlanningMessages());
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
                LocalTime.of(16, 0));
        assertEquals(List.of(expected), model.getSavedPlans());
        assertEquals(List.of(List.of(expected)), savedPlanLists);
    }

    @Test
    void selectRoute_leaveByAlreadyPassed_doesNotRollOverOrSavePlan() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        model.plan(new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO));

        RouteSelectionResult result = model.selectRoute(1);

        assertEquals(RouteSelectionResult.Status.LEAVE_NOW, result.status());
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

        RouteSelectionResult result = model.selectRoute(1);

        assertEquals(RouteSelectionResult.Status.LEAVE_NOW, result.status());
        assertTrue(model.getSavedPlans().isEmpty());
        assertTrue(savedPlanLists.isEmpty());
    }

    @Test
    void selectRoute_planSaveFails_preservesUnselectedPlanState() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        PlanStore failingPlanStore = new PlanStore() {
            @Override
            public void saveAll(List<SavedPlan> plans) {
                throw new IllegalStateException("Saved plan storage is unavailable.");
            }
        };
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock, failingPlanStore);
        PlanCommand plan = new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO);
        plan.execute(model);

        assertThrows(IllegalStateException.class, () -> model.selectRoute(1));
        assertEquals(plan, model.getPendingPlan().orElseThrow());
        assertEquals("Offline estimate", model.getPendingAlternatives().getFirst().name());
        assertTrue(model.getSelectedRecommendation().isEmpty());
        assertTrue(model.getSavedPlans().isEmpty());
    }

    @Test
    void selectRoute_departureCrossesMidnight_doesNotRollOverOrSavePlan() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T14:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        model.plan(new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(0, 10), Duration.ofMinutes(10)));

        RouteSelectionResult result = model.selectRoute(1);

        assertEquals(RouteSelectionResult.Status.LEAVE_NOW, result.status());
        assertTrue(model.getSavedPlans().isEmpty());
        assertTrue(savedPlanLists.isEmpty());
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

        assertEquals(RouteSelectionResult.Status.ROUTE_SELECTED, result.status());
        assertEquals("Second route", result.recommendation().orElseThrow().routeName());
    }

    @Test
    void plan_liveRefreshFails_replacesStaleSelectionWithDeterministicFallback() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T01:00:00Z"), ZoneId.of("Asia/Singapore"));
        PlanCommand previousPlan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ZERO);
        RouteAlternative previousRoute = new RouteAlternative("Previous live route", Duration.ofMinutes(8),
                Duration.ofMinutes(35), 1);
        AtomicInteger liveLookupCalls = new AtomicInteger();
        LiveTransitPlanner liveTransitPlanner = (origin, destination, date, time) ->
                liveLookupCalls.getAndIncrement() == 0
                        ? LiveRouteLookup.available(List.of(previousRoute))
                        : LiveRouteLookup.unavailable("Live routing is unavailable.");
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()), foundLocationResolver(),
                liveTransitPlanner, clock);
        var model = new TimeyModel(planner, new InMemoryFixedCommuteStore(), plans -> { }, clock);
        model.replacePlan(previousPlan, List.of(previousRoute), List.of("Live route available."));
        model.selectRoute(1);

        model.plan(previousPlan);

        assertEquals(2, liveLookupCalls.get());
        assertTrue(model.getSelectedRecommendation().isEmpty());
        assertEquals("Offline estimate", model.getPendingAlternatives().getFirst().name());
        assertEquals("Using a default 1-hour buffer before your target arrival time instead of live estimates.",
                model.getPlanningMessages().getLast());
    }

    @Test
    void plan_savedTimingLookupFails_preservesPreviousPlanState() {
        FixedCommuteStore failingStore = new FixedCommuteStore() {
            @Override
            public void save(FixedCommute commute) {
            }

            @Override
            public Optional<FixedCommute> find(String origin, String destination) {
                throw new IllegalStateException("Saved commute storage is unavailable.");
            }

            @Override
            public List<FixedCommute> findAll() {
                return List.of();
            }

            @Override
            public boolean remove(String origin, String destination) {
                return false;
            }
        };
        var model = TestTimeyModelFactory.create(failingStore);
        PlanCommand previousPlan = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ZERO);
        RouteAlternative previousRoute = new RouteAlternative("Previous route", Duration.ofMinutes(5),
                Duration.ofMinutes(30), 0);
        model.replacePlan(previousPlan, List.of(previousRoute), List.of("Previous route remains available."));

        PlanCommand replacementPlan = new PlanCommand("Home", "NUS", LocalTime.of(9, 0), Duration.ZERO);

        assertThrows(IllegalStateException.class, () -> model.plan(replacementPlan));
        assertEquals(previousPlan, model.getPendingPlan().orElseThrow());
        assertEquals(List.of(previousRoute), model.getPendingAlternatives());
        assertEquals(List.of("Previous route remains available."), model.getPlanningMessages());
        assertTrue(model.getSelectedRecommendation().isEmpty());
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

    private static LocationResolver foundLocationResolver() {
        return query -> LocationResolution.found(new ResolvedLocation(query, query + " address", 1.3, 103.8));
    }
}
