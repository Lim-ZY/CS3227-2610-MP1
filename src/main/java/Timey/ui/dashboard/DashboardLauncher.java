package Timey.ui.dashboard;

import javafx.application.Application;

/** Launches the JavaFX dashboard without replacing the terminal CLI entry point. */
public final class DashboardLauncher {
    private DashboardLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(TimeyDashboardApp.class, args);
    }
}
