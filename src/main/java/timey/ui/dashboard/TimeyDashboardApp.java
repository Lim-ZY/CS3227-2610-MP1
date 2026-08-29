package timey.ui.dashboard;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javafx.application.Application;
import javafx.stage.Stage;
import timey.ApplicationFactory;
import timey.config.UserPreferences;
import timey.ui.CommandLineApp;
import timey.ui.ConsoleUi;

/** JavaFX presentation shell for Timey's dashboard. */
public final class TimeyDashboardApp extends Application {
    private CommandLineApp commandLineApp;
    private StringWriter output;
    private UserPreferences userPreferences;

    @Override
    public void init() {
        output = new StringWriter();
        commandLineApp = ApplicationFactory.createCommandLineApp(
                new ConsoleUi(new BufferedReader(new StringReader("")), new PrintWriter(output, true)));
        userPreferences = ApplicationFactory.loadUserPreferences();
    }

    @Override
    public void start(Stage stage) {
        MainWindow mainWindow = new MainWindow(stage, commandLineApp, output, userPreferences);
        mainWindow.show();
    }
}
