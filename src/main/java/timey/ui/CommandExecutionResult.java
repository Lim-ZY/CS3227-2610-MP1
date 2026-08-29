package timey.ui;

/** Result of a single shared command execution. */
public record CommandExecutionResult(boolean sessionEnded, DashboardState dashboardState) {
}
