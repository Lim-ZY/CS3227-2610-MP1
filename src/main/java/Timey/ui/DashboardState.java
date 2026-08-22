package Timey.ui;

import java.util.List;
import java.util.Optional;

import Timey.domain.alert.DepartureRecommendation;
import Timey.domain.alert.ScheduledDepartureReminder;
import Timey.domain.transit.RouteAlternative;
import Timey.parser.PlanCommand;

/** Immutable command-session data that a dashboard can render without re-running planning logic. */
public record DashboardState(Optional<PlanCommand> plan, List<RouteAlternative> alternatives,
        List<String> planningMessages, Optional<DepartureRecommendation> recommendation,
        List<ScheduledDepartureReminder> reminders) {
    public DashboardState {
        plan = plan == null ? Optional.empty() : plan;
        alternatives = List.copyOf(alternatives);
        planningMessages = List.copyOf(planningMessages);
        recommendation = recommendation == null ? Optional.empty() : recommendation;
        reminders = List.copyOf(reminders);
    }
}
