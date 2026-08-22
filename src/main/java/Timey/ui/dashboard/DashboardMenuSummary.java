package Timey.ui.dashboard;

import Timey.ui.DashboardState;
import Timey.config.UserPreferences;

/** Formats the non-editable preference summaries shown in the Timey dashboard menu. */
public final class DashboardMenuSummary {
    private DashboardMenuSummary() {
    }

    public static String recentLocations(DashboardState state) {
        return state.plan().map(plan -> plan.origin() + " → " + plan.destination()).orElse("No recent plan");
    }

    public static String personalBuffer(DashboardState state, UserPreferences preferences) {
        return state.plan().map(plan -> plan.buffer().toMinutes() + " minutes (current plan)")
                .orElse(preferences.defaultDepartureBuffer().toMinutes() + " minutes (default)");
    }
}
