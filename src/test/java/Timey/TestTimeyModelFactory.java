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

/** Creates a fully wired model for tests that do not need a live route source. */
public final class TestTimeyModelFactory {
    private TestTimeyModelFactory() {
    }

    public static TimeyModel create(FixedCommuteStore fixedCommuteStore) {
        var planner = new Planner(new CommutePlanningService(new MockTransitPlanner()),
                query -> LocationResolution.unavailable("Offline"),
                (origin, destination, date, time) -> LiveRouteLookup.available(List.of()), Clock.systemUTC());
        return new TimeyModel(planner, fixedCommuteStore);
    }
}
