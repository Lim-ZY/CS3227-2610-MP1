package Timey.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import Timey.TestTimeyModelFactory;
import Timey.domain.transit.RouteAlternative;
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
}
