package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class ChooseCommandTest {
    @Test
    void execute_withoutPendingPlan_requestsPlanFirst() {
        var result = new ChooseCommand(1).execute(TestTimeyModelFactory.create(new InMemoryFixedCommuteStore()));

        assertEquals(java.util.List.of("Please create a plan before choosing a route."), result.messages());
    }

    @Test
    void execute_withPendingRoute_selectsRecommendationAndSchedulesReminder() {
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(),
                Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore")));
        new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), java.time.Duration.ZERO).execute(model);

        var result = new ChooseCommand(1).execute(model);

        assertTrue(result.messages().contains("Chosen route: Offline estimate"));
        assertTrue(result.messages().stream()
                .anyMatch(message -> message.startsWith("Departure reminder automatically set")));
        assertEquals("Offline estimate", model.getSelectedRecommendation().orElseThrow().routeName());
        assertEquals(1, model.getScheduledReminders().size());
    }

    @Test
    void execute_routeAlreadySelected_requestsNewPlanWithoutAnotherReminder() {
        var model = TestTimeyModelFactory.create(new InMemoryFixedCommuteStore(),
                Clock.fixed(Instant.parse("2026-08-21T01:30:00Z"), ZoneId.of("Asia/Singapore")));
        new PlanCommand("COM3", "VivoCity", LocalTime.of(18, 30), java.time.Duration.ZERO).execute(model);
        new ChooseCommand(1).execute(model);

        var result = new ChooseCommand(2).execute(model);

        assertEquals(java.util.List.of("A route is already selected for the current plan. "
                + "Please create a new plan before choosing another route."), result.messages());
        assertEquals(1, model.getScheduledReminders().size());
    }
}
