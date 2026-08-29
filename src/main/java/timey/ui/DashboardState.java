package timey.ui;

import java.util.List;
import java.util.Optional;

import timey.command.PlanCommand;
import timey.domain.alert.DepartureRecommendation;
import timey.domain.alert.ScheduledDepartureReminder;
import timey.domain.transit.RouteAlternative;

/** Immutable command-session data that a dashboard can render without re-running planning logic. */
public record DashboardState(Optional<PlanCommand> plan, List<RouteAlternative> alternatives,
        List<String> planningMessages, Optional<DepartureRecommendation> recommendation,
        List<ScheduledDepartureReminder> reminders) {
    /** Performs this operation. */
    public DashboardState {
        plan = plan == null ? Optional.empty() : plan;
        alternatives = List.copyOf(alternatives);
        planningMessages = List.copyOf(planningMessages);
        recommendation = recommendation == null ? Optional.empty() : recommendation;
        reminders = List.copyOf(reminders);
    }
}
