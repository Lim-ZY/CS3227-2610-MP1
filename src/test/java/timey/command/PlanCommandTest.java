package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class PlanCommandTest {
    @Test
    void execute_plansRouteAndStoresTheCurrentPlan() {
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore());
        var command = new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), Duration.ofMinutes(5));

        var result = command.execute(model);

        assertEquals(command, model.getPendingPlan().orElseThrow());
        assertEquals(2, model.getPendingAlternatives().size());
        assertTrue(result.messages().contains("From: COM3"));
        assertTrue(result.messages().contains("Choose a route with: choose 1"));
    }
}
