package Timey;

import java.time.Clock;
import java.util.List;

import Timey.domain.location.LocationResolution;
import Timey.domain.transit.LiveRouteLookup;
import Timey.infrastructure.transit.MockTransitPlanner;
import Timey.model.TimeyModel;
import Timey.planner.CommutePlanningService;
import Timey.planner.Planner;
import Timey.ports.FixedCommuteStore;
import Timey.ports.PlanStore;
import Timey.reminder.DepartureReminderService;

/** Creates a fully wired model for tests that do not need a live route source. */
public final class TestTimeyModelFactory {
    private TestTimeyModelFactory() {
    }

    public static TimeyModel create(FixedCommuteStore fixedCommuteStore) {
        return create(fixedCommuteStore, Clock.systemUTC());
    }

    public static TimeyModel create(FixedCommuteStore fixedCommuteStore, Clock clock) {
        return create(fixedCommuteStore, clock, plans -> { });
    }

    public static TimeyModel create(FixedCommuteStore fixedCommuteStore, Clock clock, PlanStore planStore) {
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()),
                query -> LocationResolution.unavailable("Offline"),
                (origin, destination, date, time) -> LiveRouteLookup.available(List.of()), clock);
        return new TimeyModel(planner, fixedCommuteStore, planStore,
                new DepartureReminderService((triggerAt, action) -> () -> { }, clock, reminder -> { }), clock);
    }
}
