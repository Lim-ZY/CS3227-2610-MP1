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
    void selectRoute_targetArrivalAlreadyPassed_savesPlanForTomorrow() {
        var savedPlanLists = new ArrayList<List<SavedPlan>>();
        Clock clock = Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneId.of("Asia/Singapore"));
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(), clock,
                plans -> savedPlanLists.add(List.copyOf(plans)));
        new PlanCommand("Admiralty MRT", "COM3", LocalTime.of(17, 0), Duration.ZERO).execute(model);

        model.selectRoute(1);

        assertEquals(LocalDate.of(2026, 8, 30), model.getSavedPlans().getFirst().date());
        assertEquals(1, savedPlanLists.size());
    }
}
