package timey.ui.dashboard;

import timey.config.UserPreferences;
import timey.ui.DashboardState;

/** Formats the non-editable preference summaries shown in the Timey dashboard menu. */
public final class DashboardMenuSummary {
    private DashboardMenuSummary() {
    }

    public static String recentLocations(DashboardState state) {
        return state.plan().map(plan -> plan.getOrigin() + " → " + plan.getDestination()).orElse("No recent plan");
    }

    /** Performs this operation. */
    public static String personalBuffer(DashboardState state, UserPreferences preferences) {
        return state.plan().map(plan -> plan.getBuffer().toMinutes() + " minutes (current plan)")
                .orElse(preferences.defaultDepartureBuffer().toMinutes() + " minutes (default)");
    }
}
