package timey.ui.dashboard;

import timey.ui.DashboardState;

/** User-facing commute status derived from a completed shared command session. */
public record DashboardCommuteStatus(String title, String message) {
    /** Performs this operation. */
    public static DashboardCommuteStatus from(DashboardState state) {
        if (state.alternatives().isEmpty()) {
            return new DashboardCommuteStatus("Waiting for a plan...",
                    "Live public transport alternatives will be requested once you execute `plan`.");
        }
        String messages = String.join(" ", state.planningMessages());
        if (messages.contains("Live public transport routes were aligned")) {
            return new DashboardCommuteStatus("Live public transport routes ready", messages);
        }
        if (messages.contains("offline estimate")) {
            return new DashboardCommuteStatus("Using deterministic fallback", messages);
        }
        return new DashboardCommuteStatus(state.alternatives().size() + " route alternatives ready", messages);
    }
}
