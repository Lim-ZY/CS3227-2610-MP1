package timey;

import java.time.Clock;
import java.util.List;

import timey.domain.location.LocationResolution;
import timey.domain.transit.LiveRouteLookup;
import timey.infrastructure.transit.MockTransitPlanner;
import timey.model.TimeyModel;
import timey.planner.CommutePlanningService;
import timey.planner.Planner;
import timey.ports.FixedCommuteStore;
import timey.ports.LiveTransitPlanner;
import timey.ports.PlanStore;

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
        LiveTransitPlanner liveTransitPlanner = (origin, destination, date, time) ->
                LiveRouteLookup.available(List.of());
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()),
                query -> LocationResolution.unavailable("Offline"), liveTransitPlanner, clock);
        return new TimeyModel(planner, fixedCommuteStore, planStore, clock);
    }
}
