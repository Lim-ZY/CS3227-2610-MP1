package timey.ui.dashboard;

import java.util.Objects;

import timey.ui.CommandFailureText;

/** Provides safe user-facing text for dashboard command failures. */
final class DashboardFailureText {
    private DashboardFailureText() {
    }

    static String commuteUpdate(Throwable failure) {
        Objects.requireNonNull(failure);
        return CommandFailureText.runtimeFailure();
    }
}
