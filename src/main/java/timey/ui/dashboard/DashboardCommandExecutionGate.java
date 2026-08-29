package timey.ui.dashboard;

import java.util.Objects;
import java.util.function.Supplier;

/** Serializes background dashboard commands that share command-session state. */
final class DashboardCommandExecutionGate {
    synchronized <T> T execute(Supplier<T> operation) {
        return Objects.requireNonNull(operation).get();
    }
}
