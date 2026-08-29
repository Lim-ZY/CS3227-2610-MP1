package timey.ui.dashboard;

import java.util.Objects;

/** Provides safe user-facing text for dashboard command failures. */
final class DashboardFailureText {
    private DashboardFailureText() {
    }

    static String commuteUpdate(Throwable failure) {
        Objects.requireNonNull(failure);
        return "Your previous plan is unchanged. Please try again.";
    }
}
